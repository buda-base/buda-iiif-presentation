package io.bdrc.iiif.presentation.resservices;

import static io.bdrc.iiif.presentation.AppConstants.CACHEPREFIX_VI;
import static io.bdrc.iiif.presentation.AppConstants.CANNOT_FIND_VOLUME_ERROR_CODE;
import static io.bdrc.iiif.presentation.AppConstants.GENERIC_APP_ERROR_CODE;
import static io.bdrc.iiif.presentation.AppConstants.GENERIC_LDS_ERROR;
import static io.bdrc.iiif.presentation.AppConstants.LDS_VOLUME_QUERY;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.riot.ResultSetMgr;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.resmodels.ImageGroupInfo;

public class ImageGroupInfoService extends ConcurrentResourceService<ImageGroupInfo> {

	private static final Logger logger = LoggerFactory.getLogger(ImageGroupInfoService.class);

	public static final ImageGroupInfoService Instance = new ImageGroupInfoService();

	ImageGroupInfoService() {
		super(CACHEPREFIX_VI);
	}

	@Override
	final public ImageGroupInfo getFromApi(final String volumeId) throws BDRCAPIException {
		logger.info("fetch volume info on LDS for {}", volumeId);
		final HttpClient httpClient = HttpClientBuilder.create().build(); // Use this instead
		final ImageGroupInfo resVolumeInfo;
		try {
	        URIBuilder builder = new URIBuilder(LDS_VOLUME_QUERY);
	        builder.setParameter("R_RES", volumeId);
	        builder.setParameter("format", "json");
	        final HttpGet request = new HttpGet(builder.build());
			final HttpResponse response = httpClient.execute(request);
			int code = response.getStatusLine().getStatusCode();
			if (code != 200) {
				throw new BDRCAPIException(500, GENERIC_LDS_ERROR, "LDS lookup returned an error for volume " + volumeId, response.toString(), "");
			}
			final InputStream body = response.getEntity().getContent();
			final ResultSet res = ResultSetMgr.read(body, ResultSetLang.RS_JSON);
			if (!res.hasNext()) {
				throw new BDRCAPIException(500, CANNOT_FIND_VOLUME_ERROR_CODE, "cannot find volume " + volumeId + " in the database");
			}
			final QuerySolution sol = res.next();
			resVolumeInfo = new ImageGroupInfo(sol, volumeId);
			if (res.hasNext()) {
				throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, "more than one volume found in the database for " + volumeId + ", this shouldn't happen");
			}
		} catch (IOException | URISyntaxException ex) {
			throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, ex);
		}
		logger.info("found volume info: {}", resVolumeInfo);
		return resVolumeInfo;
	}
}
