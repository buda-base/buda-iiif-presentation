package io.bdrc.iiif.presentation;

import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.digitalcollections.iiif.model.sharedcanvas.Collection;
import de.digitalcollections.iiif.model.sharedcanvas.Manifest;
import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.models.Identifier;
import io.bdrc.iiif.presentation.models.VolumeInfo;

@Path("/2.1.1")
public class IIIFPresentationService {

    private static final Logger logger = LoggerFactory.getLogger(IIIFPresentationService.class);
    
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{identifier}/manifest")
	// add @Context UriInfo uriInfo to the arguments to get auth header
	public Response getManifest(@PathParam("identifier") final String identifier) throws BDRCAPIException {
		final Identifier id = new Identifier(identifier, Identifier.MANIFEST_ID);
		final VolumeInfo vi = VolumeInfoService.getVolumeInfo(id.getVolumeId());
		if (vi.iiifManifest != null) {
		    logger.info("redirect manifest request for ID {} to {}", identifier, vi.iiifManifest.toString());
	        return Response.status(302) // maybe 303 or 307 would be better?
	                .header("Location", vi.iiifManifest)
	                .header("Access-Control-Allow-Origin", "*")
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
    @Path("/collection/{identifier}")
    public Collection getCollection(@PathParam("identifier") final String identifier) throws BDRCAPIException {
        final Identifier id = new Identifier(identifier, Identifier.COLLECTION_ID);
        return CollectionService.getCollectionForIdentifier(id);
    }

}
