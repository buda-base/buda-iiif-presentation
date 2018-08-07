package io.bdrc.iiif.presentation;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;

@ApplicationPath("/")
public class IIIFPresApplication extends ResourceConfig {
    
    public IIIFPresApplication() {        
        register(CommonHeadersFilter.class);
    }

}
