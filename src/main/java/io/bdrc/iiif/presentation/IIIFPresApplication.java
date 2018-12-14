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

    //static final String configPath= System.getProperty("iiifpres.configpath");
    public final static Logger log=LoggerFactory.getLogger(IIIFPresApplication.class.getName());

    public IIIFPresApplication() {

        try {
            InputStream input=IIIFPresApplication.class.getClassLoader().getResourceAsStream("iiifpres.properties");
            Properties props=new Properties();
            props.load(input);
            try {
                InputStream is = new FileInputStream("/etc/buda/share/shared-private.properties");
                props.load(is);

            }catch(Exception ex) {
                //do nothing, continue props initialization
            }
            AuthProps.init(props);
            if("true".equals(AuthProps.getProperty("useAuth"))){
                RdfAuthModel.init();
                register(IIIFPresAuthFilter.class);
            }
            ServiceCache.init();
            register(CommonHeadersFilter.class);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
