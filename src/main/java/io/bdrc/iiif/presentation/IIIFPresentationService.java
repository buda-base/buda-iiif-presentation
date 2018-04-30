package io.bdrc.iiif.presentation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import de.digitalcollections.iiif.model.sharedcanvas.Manifest;
import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.models.Identifier;

@Path("/2.1.1")
public class IIIFPresentationService {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{identifier}/manifest")
	// add @Context UriInfo uriInfo to the arguments to get auth header
	public Manifest getManifest(@PathParam("identifier") String identifier) throws BDRCAPIException {
		Identifier id = new Identifier(identifier, Identifier.MANIFEST_ID);
		return ManifestService.getManifestForIdentifier(id);
	}
//
//    @GET
//    @Produces(MediaType.APPLICATION_JSON)
//    @Path("/collection/{identifier}")
//    public Response getCollection(@PathParam("identifier") String identifier) throws BDRCAPIException {
//    }

}
