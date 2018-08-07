package io.bdrc.iiif.presentation;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.server.ResourceConfig;

@ApplicationPath("/")
@Provider
public class IIIFPresApplication extends ResourceConfig {
    
    public IIIFPresApplication() {        
        register(CommonHeadersFilter.class);
    }

}
