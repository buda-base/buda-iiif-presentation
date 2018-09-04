package io.bdrc.iiif.presentation;

import static io.bdrc.iiif.presentation.AppConstants.GENERIC_APP_ERROR_CODE;
import static io.bdrc.iiif.presentation.AppConstants.GENERIC_LDS_ERROR;

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
import io.bdrc.iiif.presentation.models.ItemInfo;

public class ItemInfoService {
    private static final Logger logger = LoggerFactory.getLogger(ItemInfoService.class);

    private static CacheAccess<String, ItemInfo> cache = null;

    static {
        try {
            cache = JCS.getInstance("default");
        } catch (CacheException e) {
            logger.error("cache initialization error, this shouldn't happen!", e);
        }
    }

    private static ItemInfo fetchLdsVolumeInfo(final String itemId) throws BDRCAPIException {
        logger.debug("fetch itemInfo on LDS for {}", itemId);
        final HttpClient httpClient = HttpClientBuilder.create().build(); //Use this instead
        final ItemInfo resItemInfo;
        final String queryUrl = "http://buda1.bdrc.io/graph/IIIFPres_itemGraph";
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
            Model m = ModelFactory.createDefaultModel();
            // TODO: prefixes
            m.read(body, null, "TURTLE");
            resItemInfo = new ItemInfo(m, itemId);
        } catch (IOException ex) {
            throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, ex);
        }
        logger.debug("found itemInfo: {}", resItemInfo.toString());
        return resItemInfo;
    }

    public static ItemInfo getItemInfo(final String itemId) throws BDRCAPIException {
        ItemInfo resItemInfo = cache.get(itemId);
        if (resItemInfo != null) {
            logger.debug("found itemInfo in cache for "+itemId);
            return resItemInfo;
        }
        resItemInfo = fetchLdsVolumeInfo(itemId);
        if (resItemInfo == null)
            return null;
        cache.put(itemId, resItemInfo);
        return resItemInfo;
    }
}
