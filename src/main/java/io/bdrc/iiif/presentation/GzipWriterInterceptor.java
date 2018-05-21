package io.bdrc.iiif.presentation;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import javax.validation.constraints.NotNull;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class GzipWriterInterceptor implements WriterInterceptor{

    public final static Logger log=LoggerFactory.getLogger(GzipWriterInterceptor.class);
    private HttpHeaders httpHeaders;

    public GzipWriterInterceptor(@Context @NotNull HttpHeaders httpHeaders) {
        this.httpHeaders = httpHeaders;
    }
    
    @Override
    public void aroundWriteTo(final WriterInterceptorContext context) throws IOException, WebApplicationException {
        final MultivaluedMap<String,String> requestHeaders =  httpHeaders.getRequestHeaders();
        final List<String> acceptEncoding = requestHeaders.get(HttpHeaders.ACCEPT_ENCODING);
        boolean process = false;
        if (acceptEncoding != null) {                        
            for (final String st : acceptEncoding) {                  
                if (st.contains("gzip")) {
                    process = true;
                }
            }
        }
        if (process) {
            log.trace("Writer Interceptor compressing");
            final MultivaluedMap<String,Object> headers = context.getHeaders();
            headers.add("Content-Encoding", "gzip");    
            final OutputStream outputStream = context.getOutputStream();
            context.setOutputStream(new GZIPOutputStream(outputStream));
            context.proceed();
        } else {            
            context.proceed();
        }
    }
}
