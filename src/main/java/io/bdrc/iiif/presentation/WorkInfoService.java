package io.bdrc.iiif.presentation;

import static io.bdrc.iiif.presentation.AppConstants.GENERIC_APP_ERROR_CODE;
import static io.bdrc.iiif.presentation.AppConstants.GENERIC_LDS_ERROR;
import static io.bdrc.iiif.presentation.AppConstants.LDS_WORKGRAPH_QUERY;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.jcs.access.CacheAccess;
import org.apache.commons.jcs.access.exception.CacheException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.models.WorkInfo;

public class WorkInfoService {
    private static final Logger logger = LoggerFactory.getLogger(WorkInfoService.class);

    private static CacheAccess<String, Object> cache = null;

    static {
        try {
            cache = ServiceCache.CACHE;
        } catch (CacheException e) {
            logger.error("cache initialization error, this shouldn't happen!", e);
        }
    }
    
    private static Model fetchLdsWorkInfoModel(final String workId) throws BDRCAPIException {
        logger.debug("fetch workInfo on LDS for {}", workId);
        final HttpClient httpClient = HttpClientBuilder.create().build(); //Use this instead
        final Model resModel;
        final String queryUrl = LDS_WORKGRAPH_QUERY;
        logger.debug("query {} with argument R_RES={}", queryUrl, workId);
        try {
            final HttpPost request = new HttpPost(queryUrl);
            // we suppose that the volumeId is well formed, which is checked by the Identifier constructor
            final StringEntity params = new StringEntity("{\"R_RES\":\""+workId+"\"}", ContentType.APPLICATION_JSON);
            request.addHeader(HttpHeaders.ACCEPT, "text/turtle");
            request.setEntity(params);
            final HttpResponse response = httpClient.execute(request);
            int code = response.getStatusLine().getStatusCode();
            if (code != 200) {
                throw new BDRCAPIException(500, GENERIC_LDS_ERROR, "LDS lookup returned http code "+code, response.toString(), "");
            }
            final InputStream body = response.getEntity().getContent();
            resModel = ModelFactory.createDefaultModel();
            // TODO: prefixes
            resModel.read(body, null, "TURTLE");
        } catch (IOException ex) {
            throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, ex);
        }
        logger.debug("found workModel: {}", resModel);
        return resModel;
    }

    public static WorkInfo getWorkInfo(final String workId) throws BDRCAPIException {
        WorkInfo resWorkInfo = (WorkInfo)cache.get(workId);
        if (resWorkInfo != null) {
            logger.debug("found workInfo in cache for "+workId);
            return resWorkInfo;
        }
        final Model workInfoModel = fetchLdsWorkInfoModel(workId);
        resWorkInfo = new WorkInfo(workInfoModel, workId);
        cache.put(workId, resWorkInfo);
        return resWorkInfo;
    }

    public static final Map<String,CompletableFuture<WorkInfo>> futures = new ConcurrentHashMap<>();
    
    /* Function returning a CompletableFuture and thus allowing many different calls
     * to iiif-presentation to try to access a non-cached WorkInfo at the same time
     * while doing only one request. 
     * 
     * What it prevents is the following case. We assume that we're using a naive
     * function, not this one. let's say an API call to lds-pdi takes
     * 100ms. Let's say we ask for all the volumes of the Kangyur at the same time to
     * iiif-presentation (something which actually happens). This means that the following
     * happens:
     *  - first request to iiif-presentation: 
     *      no workinfo cached, making a request to lds-pdi, takes 100ms
     *  - second request to iiif-presentation (2ms after the first one):
     *      the call to lds-pdi hasn't finished yet, so there is still no cache,
     *      so making another request to lds-pdi
     *  - etc.
     *  
     *  which can lead to a large load of requests to lds-pdi, making all the requests
     *  much longer and stressing lds-pdi.
     *  
     *  With this function the following happens (conceptually):
     *   - first request to iiif-presentation:
     *       no workinfo cached, making adding the request to lds-pdi in the map of requests
     *   - second request to iiif-presentation:
     *       no workinfo cached, but we see that the maps of requests contains
     *       one request for this resource, so we just wait for it to return
     *   - etc.
     *   
     *  which means just one request is made instead of 100.
     * 
     */
    public static CompletableFuture<WorkInfo> getWorkInfoAsync(final String workId) {
        WorkInfo resWorkInfo = (WorkInfo)cache.get(workId);
        if (resWorkInfo != null) {
            logger.debug("found workInfo in cache for "+workId);
            CompletableFuture<WorkInfo> resCached = new CompletableFuture<>();
            resCached.complete(resWorkInfo);
            return resCached;
        }
        // unintuitive way to perform the (necessary) atomic operation in the list
        CompletableFuture<WorkInfo> res = new CompletableFuture<>(); 
        CompletableFuture<WorkInfo> resFromList = futures.putIfAbsent(workId, res);
        if (resFromList != null) {
            // this is the case of all the threads trying to access the resource
            // except the first one
            return resFromList;
        }
        try {
            final Model workInfoModel = fetchLdsWorkInfoModel(workId);
            resWorkInfo = new WorkInfo(workInfoModel, workId);
        } catch (BDRCAPIException e) {
            res.completeExceptionally(e);
            return res;
        }
        cache.put(workId, resWorkInfo);
        res.complete(resWorkInfo);
        futures.remove(workId);
        return res;
    }
    
}
