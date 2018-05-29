package io.bdrc.iiif.presentation;

import java.lang.reflect.Method;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.Provider;


@Provider
public class CacheControlFilterFactory implements DynamicFeature {
    
    private final static String MAX_AGE = "max-age=";
    private final static String NO_CACHE = "no-cache";
    
    @Override
    public void configure(final ResourceInfo resourceInfo, final FeatureContext context) {
        final Method resourceMethod = resourceInfo.getResourceMethod();
        if (resourceMethod.isAnnotationPresent(JerseyCacheControl.class) ) {
            final JerseyCacheControl ccontrol = resourceMethod.getAnnotation(JerseyCacheControl.class);
            if(ccontrol.noCache()) {
                context.register(new CacheResponseFilter(NO_CACHE));
            }else {
                long seconds = ccontrol.maxAge();
                context.register(new CacheResponseFilter(MAX_AGE + seconds));
            }
        } 
    }

    private static class CacheResponseFilter implements ContainerResponseFilter {
        private final String headerValue;

        CacheResponseFilter(final String headerValue) {
            this.headerValue = headerValue;
        }

        @Override
        public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext) {
            responseContext.getHeaders().putSingle(HttpHeaders.CACHE_CONTROL, headerValue);
        }
    }
}