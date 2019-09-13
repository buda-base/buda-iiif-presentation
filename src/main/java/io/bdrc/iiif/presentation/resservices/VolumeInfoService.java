package io.bdrc.iiif.presentation.resservices;

import static io.bdrc.iiif.presentation.AppConstants.CANNOT_FIND_VOLUME_ERROR_CODE;
import static io.bdrc.iiif.presentation.AppConstants.GENERIC_APP_ERROR_CODE;
import static io.bdrc.iiif.presentation.AppConstants.GENERIC_LDS_ERROR;
import static io.bdrc.iiif.presentation.AppConstants.LDS_VOLUME_QUERY;
import static io.bdrc.iiif.presentation.AppConstants.CACHEPREFIX_VI;

import java.io.IOException;
import java.io.InputStream;

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
import io.bdrc.iiif.presentation.resmodels.VolumeInfo;

public class VolumeInfoService extends ConcurrentResourceService<VolumeInfo> {

    private static final Logger logger = LoggerFactory.getLogger(VolumeInfoService.class);
    
    public static final VolumeInfoService Instance = new VolumeInfoService();

    VolumeInfoService() {
        super(ServiceCache.CACHE, CACHEPREFIX_VI);
    }
    
    @Override
    final public VolumeInfo getFromApi(final String volumeId) throws BDRCAPIException {
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
                throw new BDRCAPIException(500, GENERIC_LDS_ERROR, "LDS lookup returned an error for volume "+volumeId, response.toString(), "");
            }
            final InputStream body = response.getEntity().getContent();
            final ResultSet res = ResultSetMgr.read(body, ResultSetLang.SPARQLResultSetJSON);
            if (!res.hasNext()) {
                throw new BDRCAPIException(500, CANNOT_FIND_VOLUME_ERROR_CODE, "cannot find volume "+volumeId+" in the database");
            }
            final QuerySolution sol = res.next();
            resVolumeInfo = new VolumeInfo(sol);
            if (res.hasNext()) {
                throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, "more than one volume found in the database for "+volumeId+", this shouldn't happen");
            }
        } catch (IOException ex) {
            throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, ex);
        }
        logger.info("found volume info: {}", resVolumeInfo);
        return resVolumeInfo;
    }
}
