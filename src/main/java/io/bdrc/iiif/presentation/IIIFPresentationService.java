package io.bdrc.iiif.presentation;

import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.digitalcollections.iiif.model.sharedcanvas.Collection;
import de.digitalcollections.iiif.model.sharedcanvas.Manifest;
import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.models.Identifier;
import io.bdrc.iiif.presentation.models.VolumeInfo;

@Path("/")
public class IIIFPresentationService {

    private static final Logger logger = LoggerFactory.getLogger(IIIFPresentationService.class);
    static final int ACCESS_CONTROL_MAX_AGE_IN_SECONDS = 24 * 60 * 60;
    
    @GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/2.1.1/{identifier}/manifest")
	@JerseyCacheControl()
	// add @Context UriInfo uriInfo to the arguments to get auth header
	public Response getManifest(@PathParam("identifier") final String identifier,
	        @Context ContainerResponseContext response) throws BDRCAPIException {
        //applyCors(response);
		final Identifier id = new Identifier(identifier, Identifier.MANIFEST_ID);
		final VolumeInfo vi = VolumeInfoService.getVolumeInfo(id.getVolumeId());
		if (vi.iiifManifest != null) {
		    logger.info("redirect manifest request for ID {} to {}", identifier, vi.iiifManifest.toString());
	        return Response.status(302) // maybe 303 or 307 would be better?
	                .header("Location", vi.iiifManifest)
	                .build();
		}
		final Manifest resmanifest = ManifestService.getManifestForIdentifier(id, vi); 
        final StreamingOutput stream = new StreamingOutput() {
            public void write(final OutputStream os) throws IOException, WebApplicationException {
                IIIFApiObjectMapperProvider.writer.writeValue(os , resmanifest);                    
            }
        };
		return Response.ok(stream).build();
	}

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/2.1.1/collection/{identifier}")
    @JerseyCacheControl()
    public Collection getCollection(@PathParam("identifier") final String identifier,
            @Context ContainerResponseContext response) throws BDRCAPIException { 
        //applyCors(response);
        final Identifier id = new Identifier(identifier, Identifier.COLLECTION_ID);          
        return CollectionService.getCollectionForIdentifier(id);
    }
    
    /*public void applyCors(ContainerResponseContext response) {
        response.getHeaders().add("Access-Control-Allow-Origin", "*");
        response.getHeaders().add("Access-Control-Allow-Credentials", "true");
        response.getHeaders().add("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS");
        response.getHeaders().add("Access-Control-Allow-Headers", "Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers, Authorization, Keep-Alive, User-Agent, If-Modified-Since, If-None-Match, Cache-Control");
        response.getHeaders().add("Access-Control-Expose-Headers", "Cache-Control, ETag, Last-Modified, Content-Type, Cache-Control, Vary, Access-Control-Max-Age");
        response.getHeaders().add("Access-Control-Max-Age", ACCESS_CONTROL_MAX_AGE_IN_SECONDS);
    }*/

}
