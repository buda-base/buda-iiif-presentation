package io.bdrc.iiif.presentation;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.auth.AuthProps;
import io.bdrc.auth.rdf.RdfAuthModel;

@ApplicationPath("/")
@Provider
public class IIIFPresApplication extends ResourceConfig {

    static final String configPath= System.getProperty("iiifpres.configpath");
    public final static Logger log=LoggerFactory.getLogger(IIIFPresApplication.class.getName());

    public IIIFPresApplication() {
        InputStream is;
        try {
            is = new FileInputStream(configPath+"iiifpres-auth.properties");
            Properties props=new Properties();
            props.load(is);
            AuthProps.init(props);
            RdfAuthModel.init();
            ServiceCache.init();
            register(CommonHeadersFilter.class);
            register(IIIFPresAuthFilter.class);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
