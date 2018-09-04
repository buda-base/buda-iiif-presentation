package io.bdrc.iiif.presentation;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.server.ResourceConfig;

import io.bdrc.auth.rdf.RdfAuthModel;

@ApplicationPath("/")
@Provider
public class IIIFPresApplication extends ResourceConfig {

    public IIIFPresApplication() {
        RdfAuthModel.init();
        register(CommonHeadersFilter.class);
        register(IIIFPresAuthFilter.class);
    }

}
