package io.bdrc.iiif.presentation.resservices;

import static io.bdrc.iiif.presentation.AppConstants.CACHEPREFIX_WI;
import static io.bdrc.iiif.presentation.AppConstants.GENERIC_APP_ERROR_CODE;
import static io.bdrc.iiif.presentation.AppConstants.GENERIC_LDS_ERROR;
import static io.bdrc.iiif.presentation.AppConstants.LDS_INSTANCEGRAPH_QUERY;

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

import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.resmodels.InstanceInfo;

public class InstanceInfoService extends ConcurrentResourceService<InstanceInfo> {
	private static final Logger logger = LoggerFactory.getLogger(InstanceInfoService.class);

	public static final InstanceInfoService Instance = new InstanceInfoService();

	InstanceInfoService() {
		super(CACHEPREFIX_WI);
	}

	final public InstanceInfo getFromApi(final String workId) throws BDRCAPIException {
		logger.debug("fetch workInfo on LDS for {}", workId);
		final HttpClient httpClient = HttpClientBuilder.create().build(); // Use this instead
		final Model resModel;
		final String queryUrl = LDS_INSTANCEGRAPH_QUERY;
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
				// should indicate the actual LDS http code (mostly 404 instead of 500)
				throw new BDRCAPIException(code, GENERIC_LDS_ERROR, "LDS lookup returned http code " + code, response.toString(), "");
			}
			final InputStream body = response.getEntity().getContent();
			resModel = ModelFactory.createDefaultModel();
			// TODO: prefixes
			//body.transferTo(System.out);
			resModel.read(body, null, "TURTLE");
		} catch (IOException | URISyntaxException ex) {
			throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, ex);
		}
		logger.debug("found workModel: {}", resModel);
		return new InstanceInfo(resModel, workId);
	}

}
