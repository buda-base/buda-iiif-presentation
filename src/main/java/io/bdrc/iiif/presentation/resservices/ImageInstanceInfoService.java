package io.bdrc.iiif.presentation.resservices;

import static io.bdrc.iiif.presentation.AppConstants.CACHEPREFIX_II;
import static io.bdrc.iiif.presentation.AppConstants.GENERIC_APP_ERROR_CODE;
import static io.bdrc.iiif.presentation.AppConstants.GENERIC_LDS_ERROR;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.iiif.presentation.AppConstants;
import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.resmodels.ImageInstanceInfo;

public class ImageInstanceInfoService extends ConcurrentResourceService<ImageInstanceInfo> {
	private static final Logger logger = LoggerFactory.getLogger(ImageInstanceInfoService.class);

	public static final ImageInstanceInfoService Instance = new ImageInstanceInfoService();

	ImageInstanceInfoService() {
		super(CACHEPREFIX_II);
	}

	@Override
	final public ImageInstanceInfo getFromApi(final String itemId) throws BDRCAPIException {
		logger.debug("fetch itemInfo on LDS for {}", itemId);
		final ImageInstanceInfo resItemInfo;
		final String queryUrl = AppConstants.LDS_IMAGEINSTANCEGRAPH_QUERY;
		logger.debug("query {} with argument R_RES={}", queryUrl, itemId);
		try {
		    URIBuilder builder = new URIBuilder(queryUrl);
            builder.setParameter("R_RES", itemId);
            builder.setParameter("format", "ttl");
            final HttpGet request = new HttpGet(builder.build());
			request.addHeader(HttpHeaders.ACCEPT, "text/turtle");
			try (CloseableHttpResponse response = LdsHttpClient.execute(request)) {
				int code = response.getStatusLine().getStatusCode();
				if (code != 200) {
					throw new BDRCAPIException(500, GENERIC_LDS_ERROR, "LDS lookup returned an error", "request:\n" + request.toString() + "\nresponse:\n" + response.toString(), "");
				}
				final InputStream body = response.getEntity().getContent();
				final Model m = ModelFactory.createDefaultModel();
				// TODO: prefixes
				m.read(body, null, "TURTLE");
				resItemInfo = new ImageInstanceInfo(m, itemId);
			}
		} catch (IOException | URISyntaxException ex) {
			throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, ex);
		}
		logger.debug("found itemInfo: {}", resItemInfo);
		return resItemInfo;
	}
}
