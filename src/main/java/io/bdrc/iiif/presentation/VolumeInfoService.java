package io.bdrc.iiif.presentation;

import static io.bdrc.iiif.presentation.AppConstants.*;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.jcs.JCS;
import org.apache.commons.jcs.access.CacheAccess;
import org.apache.commons.jcs.access.exception.CacheException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.riot.ResultSetMgr;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.models.VolumeInfo;

public class VolumeInfoService {
    
    private static final Logger logger = LoggerFactory.getLogger(VolumeInfoService.class);
    
    private static CacheAccess<String, VolumeInfo> cache = null;
    
    static {
        try {
            cache = JCS.getInstance("default");
        } catch (CacheException e) {
            logger.error("cache initialization error, this shouldn't happen!", e);
        }
    }
    
    private static VolumeInfo fetchLdsVolumeInfo(final String volumeId) throws BDRCAPIException {
        logger.debug("fetch volume info on LDS for {}", volumeId);
        final HttpClient httpClient = HttpClientBuilder.create().build(); //Use this instead 
        final VolumeInfo resVolumeInfo;
        try {
            final HttpPost request = new HttpPost("http://purl.bdrc.io/query/IIIFPres_volumeInfo");
            // we suppose that the volumeId is well formed, which is checked by the Identifier constructor
            final StringEntity params = new StringEntity("{\"R_RES\":\""+volumeId+"\"}", ContentType.APPLICATION_JSON);
            //request.addHeader(HttpHeaders.ACCEPT, "application/json");
            request.setEntity(params);
            final HttpResponse response = httpClient.execute(request);
            int code = response.getStatusLine().getStatusCode();
            if (code != 200) {
                throw new BDRCAPIException(500, GENERIC_LDS_ERROR, "LDS lookup returned an error", response.toString(), "");
            }
            final InputStream body = response.getEntity().getContent();
            final ResultSet res = ResultSetMgr.read(body, ResultSetLang.SPARQLResultSetJSON);
            if (!res.hasNext()) {
                throw new BDRCAPIException(500, CANNOT_FIND_VOLUME_ERROR_CODE, "cannot find volume in the database");
            }
            final QuerySolution sol = res.next();
            resVolumeInfo = new VolumeInfo(sol);
            if (res.hasNext()) {
                throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, "more than one volume found in the database, this shouldn't happen");
            }
        } catch (IOException ex) {
            throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, ex);
        }
        logger.debug("found volume info: {}", resVolumeInfo.toString());
        return resVolumeInfo;
    }
    
    public static VolumeInfo getVolumeInfo(final String volumeId) throws BDRCAPIException {
        VolumeInfo resVolumeInfo = cache.get(volumeId);
        if (resVolumeInfo != null) {
            logger.debug("found volumeInfo in cache for "+volumeId);
            return resVolumeInfo;
        }
        resVolumeInfo = fetchLdsVolumeInfo(volumeId);
        if (resVolumeInfo == null)
            return null;
        cache.put(volumeId, resVolumeInfo);
        return resVolumeInfo;
    }
    
}
