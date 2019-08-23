package io.bdrc.iiif.presentation.resservices;

import static io.bdrc.iiif.presentation.AppConstants.GENERIC_APP_ERROR_CODE;
import static io.bdrc.iiif.presentation.AppConstants.GENERIC_LDS_ERROR;

import java.io.IOException;
import java.io.InputStream;

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

import io.bdrc.iiif.presentation.AppConstants;
import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.resmodels.ItemInfo;

public class ItemInfoService extends ConcurrentResourceService<ItemInfo> {
    private static final Logger logger = LoggerFactory.getLogger(ItemInfoService.class);
    
    public static final ItemInfoService Instance = new ItemInfoService();

    @Override
    final ItemInfo getFromApi(final String itemId) throws BDRCAPIException {
        logger.debug("fetch itemInfo on LDS for {}", itemId);
        final HttpClient httpClient = HttpClientBuilder.create().build(); //Use this instead
        final ItemInfo resItemInfo;
        final String queryUrl = AppConstants.LDS_ITEMGRAPH_QUERY;
        logger.debug("query {} with argument R_RES={}", queryUrl, itemId);
        try {
            final HttpPost request = new HttpPost(queryUrl);
            // we suppose that the volumeId is well formed, which is checked by the Identifier constructor
            final StringEntity params = new StringEntity("{\"R_RES\":\""+itemId+"\"}", ContentType.APPLICATION_JSON);
            request.addHeader(HttpHeaders.ACCEPT, "text/turtle");
            request.setEntity(params);
            final HttpResponse response = httpClient.execute(request);
            int code = response.getStatusLine().getStatusCode();
            if (code != 200) {
                throw new BDRCAPIException(500, GENERIC_LDS_ERROR, "LDS lookup returned an error", "request:\n"+request.toString()+"\nresponse:\n"+response.toString(), "");
            }
            final InputStream body = response.getEntity().getContent();
            final Model m = ModelFactory.createDefaultModel();
            // TODO: prefixes
            m.read(body, null, "TURTLE");
            resItemInfo = new ItemInfo(m, itemId);
        } catch (IOException ex) {
            throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, ex);
        }
        logger.debug("found itemInfo: {}", resItemInfo);
        return resItemInfo;
    }
}
