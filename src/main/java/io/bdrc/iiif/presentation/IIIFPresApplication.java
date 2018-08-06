package io.bdrc.iiif.presentation;

import org.glassfish.jersey.server.ResourceConfig;

public class IIIFPresApplication extends ResourceConfig {
    
    public IIIFPresApplication() {
        register(CommonHeadersFilter.class);
    }

}
