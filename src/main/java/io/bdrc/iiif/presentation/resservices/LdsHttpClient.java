package io.bdrc.iiif.presentation.resservices;

import java.io.IOException;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import io.bdrc.iiif.presentation.AppConstants;

final class LdsHttpClient {

    private static final RequestConfig REQUEST_CONFIG = RequestConfig.custom()
            .setConnectTimeout(AppConstants.UPSTREAM_CONNECT_TIMEOUT_MS)
            .setConnectionRequestTimeout(AppConstants.UPSTREAM_CONNECT_TIMEOUT_MS)
            .setSocketTimeout(AppConstants.UPSTREAM_READ_TIMEOUT_MS)
            .build();

    private static final CloseableHttpClient CLIENT = HttpClients.custom()
            .setDefaultRequestConfig(REQUEST_CONFIG)
            .setMaxConnTotal(50)
            .setMaxConnPerRoute(50)
            .build();

    private LdsHttpClient() {
    }

    static CloseableHttpResponse execute(final HttpGet request) throws IOException {
        return CLIENT.execute(request);
    }
}
