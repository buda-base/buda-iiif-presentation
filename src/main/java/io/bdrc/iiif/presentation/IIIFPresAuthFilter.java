package io.bdrc.iiif.presentation;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.auth.Access;
import io.bdrc.auth.TokenValidation;
import io.bdrc.auth.UserProfile;
import io.bdrc.auth.model.Endpoint;

@Provider
@PreMatching
public class IIIFPresAuthFilter implements ContainerRequestFilter {

    public final static Logger log=LoggerFactory.getLogger(IIIFPresAuthFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        String token=getToken(ctx.getHeaderString("Authorization"));
        TokenValidation validation=null;
        UserProfile prof=null;
        if(token !=null) {
            //User is logged on
            //Getting his profile
            validation=new TokenValidation(token);
            prof=validation.getUser();
            ctx.setProperty("access", new Access(prof,new Endpoint()));
        }else {
            ctx.setProperty("access", new Access());
        }
    }

    String getToken(String header) {
        try {
            if(header!=null) {
                return header.split(" ")[1];
            }
        }
        catch(Exception ex) {
            log.error(ex.getMessage());
            return null;
        }
        return null;
    }
}