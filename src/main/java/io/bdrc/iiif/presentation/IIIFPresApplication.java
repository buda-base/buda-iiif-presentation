package io.bdrc.iiif.presentation;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.server.ResourceConfig;

import io.bdrc.auth.AuthProps;
import io.bdrc.auth.rdf.RdfAuthModel;

@ApplicationPath("/")
@Provider
public class IIIFPresApplication extends ResourceConfig {

    public final static String AUTH_PROPS_FILE="/etc/buda/iiifpres/iiifpres-auth.properties";

    public IIIFPresApplication() {
        AuthProps.init(AUTH_PROPS_FILE);
        RdfAuthModel.init();
        ServiceCache.init();
        register(CommonHeadersFilter.class);
        register(IIIFPresAuthFilter.class);
    }

}
