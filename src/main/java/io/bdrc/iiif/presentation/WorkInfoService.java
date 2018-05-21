package io.bdrc.iiif.presentation;

import static io.bdrc.iiif.presentation.AppConstants.*;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.jcs.JCS;
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
    
    private static CacheAccess<String, WorkInfo> cache = null;
    
    static {
        try {
            cache = JCS.getInstance("default");
        } catch (CacheException e) {
            logger.error("cache initialization error, this shouldn't happen!", e);
        }
    }
    
    private static WorkInfo fetchLdsWorkInfo(final String workId) throws BDRCAPIException {
        logger.debug("fetch workInfo on LDS for {}", workId);
        final HttpClient httpClient = HttpClientBuilder.create().build(); //Use this instead 
        final WorkInfo resWorkInfo;
        final String queryUrl = "http://buda1.bdrc.io/graph/IIIFPres_workGraph_noItem";
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
                throw new BDRCAPIException(500, GENERIC_LDS_ERROR, "LDS lookup returned an error", response.toString(), "");
            }
            final InputStream body = response.getEntity().getContent();
            Model m = ModelFactory.createDefaultModel();
            // TODO: prefixes
            m.read(body, null, "TURTLE");
            resWorkInfo = new WorkInfo(m, workId);
        } catch (IOException ex) {
            throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, ex);
        }
        logger.debug("found workInfo: {}", resWorkInfo.toString());
        return resWorkInfo;
    }
    
    public static WorkInfo getWorkInfo(final String workId) throws BDRCAPIException {
        WorkInfo resWorkInfo = cache.get(workId);
        if (resWorkInfo != null) {
            logger.debug("found workInfo in cache for "+workId);
            return resWorkInfo;
        }
        resWorkInfo = fetchLdsWorkInfo(workId);
        if (resWorkInfo == null)
            return null;
        cache.put(workId, resWorkInfo);
        return resWorkInfo;
    }
}
