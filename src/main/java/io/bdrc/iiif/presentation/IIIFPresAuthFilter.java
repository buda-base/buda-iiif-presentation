package io.bdrc.iiif.presentation;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response;
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
    public void filter(final ContainerRequestContext ctx) throws IOException {
        final String token = getToken(ctx.getHeaderString("Authorization"));
        if (token != null) {
            //User is logged on
            //Getting his profile
            final TokenValidation validation = new TokenValidation(token);
            final UserProfile prof = validation.getUser();
            ctx.setProperty("access", new Access(prof, new Endpoint()));
        } else {
            ctx.setProperty("access", new Access());
        }
    }


    void abort(ContainerRequestContext ctx) {
        ctx.abortWith(Response.status(Response.Status.FORBIDDEN)
                .entity("access to this resource is restricted")
                .build());
    }

    String getToken(final String header) {
        try {
            if (header != null) {
                return header.split(" ")[1];
            }
        }
        catch (Exception ex) {
            log.error(ex.getMessage());
            return null;
        }
        return null;
    }
}