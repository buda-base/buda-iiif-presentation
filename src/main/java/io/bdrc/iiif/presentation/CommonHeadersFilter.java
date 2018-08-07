package io.bdrc.iiif.presentation;


import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

@Provider
public class CommonHeadersFilter implements ContainerResponseFilter {

    static final int ACCESS_CONTROL_MAX_AGE_IN_SECONDS = 24 * 60 * 60;
    
    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        response.getHeaders().add("Access-Control-Allow-Origin", "*");
        response.getHeaders().add("Access-Control-Allow-Credentials", "true");
        response.getHeaders().add("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS");
        response.getHeaders().add("Access-Control-Allow-Headers", "Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers, Authorization, Keep-Alive, User-Agent, If-Modified-Since, If-None-Match, Cache-Control");
        response.getHeaders().add("Access-Control-Expose-Headers", "Cache-Control, ETag, Last-Modified, Content-Type, Cache-Control, Vary, Access-Control-Max-Age");
        response.getHeaders().add("Access-Control-Max-Age", ACCESS_CONTROL_MAX_AGE_IN_SECONDS);        
    }
}