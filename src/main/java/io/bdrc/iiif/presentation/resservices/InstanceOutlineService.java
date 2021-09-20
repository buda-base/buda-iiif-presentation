package io.bdrc.iiif.presentation.resservices;

import static io.bdrc.iiif.presentation.AppConstants.CACHEPREFIX_WO;
import static io.bdrc.iiif.presentation.AppConstants.GENERIC_APP_ERROR_CODE;
import static io.bdrc.iiif.presentation.AppConstants.GENERIC_LDS_ERROR;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.iiif.presentation.AppConstants;
import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.resmodels.InstanceOutline;

public class InstanceOutlineService extends ConcurrentResourceService<InstanceOutline> {
	private static final Logger logger = LoggerFactory.getLogger(InstanceOutlineService.class);

	public static final InstanceOutlineService Instance = new InstanceOutlineService();

	InstanceOutlineService() {
		super(CACHEPREFIX_WO);
	}

	@Override
	final public InstanceOutline getFromApi(final String workId) throws BDRCAPIException {
		logger.debug("fetch workOutline on LDS for {}", workId);
		final HttpClient httpClient = HttpClientBuilder.create().build();
		final InstanceOutline res;
		final String queryUrl = AppConstants.LDS_INSTANCEOUTLINE_QUERY;
		logger.debug("query {} with argument R_RES={}", queryUrl, workId);
		try {
	        URIBuilder builder = new URIBuilder(queryUrl);
	        builder.setParameter("R_RES", workId);
	        builder.setParameter("format", "ttl");
	        final HttpGet request = new HttpGet(builder.build());
	        request.addHeader(HttpHeaders.ACCEPT, "text/turtle");
			final HttpResponse response = httpClient.execute(request);
			int code = response.getStatusLine().getStatusCode();
			if (code != 200) {
				throw new BDRCAPIException(500, GENERIC_LDS_ERROR, "LDS lookup returned an error", "request:\n" + request.toString() + "\nresponse:\n" + response.toString(), "");
			}
			final InputStream body = response.getEntity().getContent();
			final Model m = ModelFactory.createDefaultModel();
			m.read(body, null, "TURTLE");
			res = new InstanceOutline(m, workId);
		} catch (IOException | URISyntaxException ex) {
			throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, ex);
		}
		logger.debug("found itemInfo: {}", res);
		return res;
	}
}
