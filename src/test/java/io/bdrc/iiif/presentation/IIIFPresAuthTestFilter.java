package io.bdrc.iiif.presentation;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import io.bdrc.auth.Access;
import io.bdrc.auth.UserProfile;
import io.bdrc.auth.model.Endpoint;

@Provider
@PreMatching
public class IIIFPresAuthTestFilter implements ContainerRequestFilter {

    public final static Logger log=LoggerFactory.getLogger(IIIFPresAuthTestFilter.class.getName());

    @Override
    public void filter(final ContainerRequestContext ctx) throws IOException {
        final String token = getToken(ctx.getHeaderString("Authorization"));
        if (token != null) {
            //User is logged on
            //Getting his profile
            //User is logged on
            //Getting his profile
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256("secret")).build();
            DecodedJWT jwt=verifier.verify(token);
            UserProfile prof=new UserProfile(jwt);
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

    static String getToken(final String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            log.error("invalid Authorization header: {}", header);
            return null;
        }
        return header.substring(7);
    }
}