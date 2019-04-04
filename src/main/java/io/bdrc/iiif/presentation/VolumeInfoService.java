package io.bdrc.iiif.presentation;

import static io.bdrc.iiif.presentation.AppConstants.CANNOT_FIND_VOLUME_ERROR_CODE;
import static io.bdrc.iiif.presentation.AppConstants.GENERIC_APP_ERROR_CODE;
import static io.bdrc.iiif.presentation.AppConstants.GENERIC_LDS_ERROR;
import static io.bdrc.iiif.presentation.AppConstants.LDS_VOLUME_OUTLINE_QUERY;
import static io.bdrc.iiif.presentation.AppConstants.LDS_VOLUME_QUERY;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.jcs.access.CacheAccess;
import org.apache.commons.jcs.access.exception.CacheException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.ResultSetMgr;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.models.VolumeInfo;

public class VolumeInfoService {

    private static final Logger logger = LoggerFactory.getLogger(VolumeInfoService.class);

    private static final String WITH_OUTLINE_SUFFIX = "-withoutline"; // interestingly ambiguous

    private static CacheAccess<String, Object> cache = null;

    static {
        try {
            cache = ServiceCache.CACHE;
        } catch (CacheException e) {
            logger.error("cache initialization error, this shouldn't happen!", e);
        }
    }

    private static VolumeInfo fetchLdsVolumeInfo(final String volumeId) throws BDRCAPIException {
        logger.info("fetch volume info on LDS for {}", volumeId);
        final HttpClient httpClient = HttpClientBuilder.create().build(); // Use this instead
        final VolumeInfo resVolumeInfo;
        try {
            final HttpPost request = new HttpPost(LDS_VOLUME_QUERY);
            // we suppose that the volumeId is well formed, which is checked by the
            // Identifier constructor
            final StringEntity params = new StringEntity("{\"R_RES\":\"" + volumeId + "\"}", ContentType.APPLICATION_JSON);
            // request.addHeader(HttpHeaders.ACCEPT, "application/json");
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
        logger.info("found volume info: {}", resVolumeInfo);
        return resVolumeInfo;
    }

    public static VolumeInfo fetchLdsVolumeOutline(final String volumeId) throws BDRCAPIException {
        logger.info("fetch volume info with outline on LDS for {}", volumeId);
        final HttpClient httpClient = HttpClientBuilder.create().build(); // Use this instead
        final VolumeInfo resVolumeInfo;
        try {
            final HttpPost request = new HttpPost(LDS_VOLUME_OUTLINE_QUERY);
            // we suppose that the volumeId is well formed, which is checked by the
            // Identifier constructor
            final StringEntity params = new StringEntity("{\"R_RES\":\"" + volumeId + "\"}", ContentType.APPLICATION_JSON);
            request.addHeader(HttpHeaders.ACCEPT, "text/turtle");
            request.setEntity(params);
            final HttpResponse response = httpClient.execute(request);
            final int code = response.getStatusLine().getStatusCode();
            if (code != 200) {
                throw new BDRCAPIException(500, GENERIC_LDS_ERROR, "LDS lookup returned an error", response.toString(), "");
            }
            final InputStream body = response.getEntity().getContent();
            Model m = ModelFactory.createDefaultModel();
            // TODO: prefixes
            m.read(body, null, "TURTLE");
            resVolumeInfo = new VolumeInfo(m, volumeId);
        } catch (IOException ex) {
            throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, ex);
        }
        logger.info("found volume info with outline: {}", resVolumeInfo);
        return resVolumeInfo;
    }

    public static VolumeInfo getVolumeInfo(final String volumeId, final boolean withOutline) throws BDRCAPIException {
        logger.info("getting volume info for {}, with outline: {}", volumeId, withOutline);
        VolumeInfo resVolumeInfo = (VolumeInfo) cache.get(volumeId + WITH_OUTLINE_SUFFIX);
        if (resVolumeInfo != null) {
            logger.info("found volumeInfo with outline in cache for {}", volumeId);
            return resVolumeInfo;
        }
        if (!withOutline) {
            resVolumeInfo = (VolumeInfo) cache.get(volumeId);
            if (resVolumeInfo != null) {
                logger.info("found volumeInfo in cache for {}", volumeId);
                return resVolumeInfo;
            }
        }
        if (withOutline) {
            resVolumeInfo = fetchLdsVolumeOutline(volumeId);
        } else {
            resVolumeInfo = fetchLdsVolumeInfo(volumeId);
        }
        if (resVolumeInfo == null)
            return null;
        if (withOutline) {
            cache.put(volumeId + WITH_OUTLINE_SUFFIX, resVolumeInfo);
        } else {
            cache.put(volumeId, resVolumeInfo);
        }
        return resVolumeInfo;
    }

}
