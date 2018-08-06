package io.bdrc.iiif.presentation;


import java.io.IOException;

import javax.inject.Singleton;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

@Singleton
@Provider
public class CommonHeadersFilter implements ContainerResponseFilter {

    static final int ACCESS_CONTROL_MAX_AGE_IN_SECONDS = 24 * 60 * 60;
    
    @Override
    public void filter(final ContainerRequestContext request, final ContainerResponseContext response) throws IOException {
        final MultivaluedMap<String, Object> headers = response.getHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Credentials", "true");
        headers.add("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers, Authorization, Keep-Alive, User-Agent, If-Modified-Since, If-None-Match, Cache-Control");
        headers.add("Access-Control-Expose-Headers", "Cache-Control, ETag, Last-Modified, Content-Type, Cache-Control, Vary, Access-Control-Max-Age");
        headers.add("Access-Control-Max-Age", ACCESS_CONTROL_MAX_AGE_IN_SECONDS);
    }
}