package io.bdrc.iiif.presentation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import de.digitalcollections.iiif.model.sharedcanvas.Manifest;
import io.bdrc.iiif.presentation.AuthService.AuthType;
import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import static io.bdrc.iiif.presentation.AppConstants.*;

@Path("/2.1.1")
public class IIIFPresentationService {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{identifier}/manifest")
	// add @Context UriInfo uriInfo to the arguments to get auth header
	public Manifest getManifest(@PathParam("identifier") String identifier) throws BDRCAPIException {
		Identifier id = new Identifier(identifier, Identifier.MANIFEST_ID);
		AuthType auth = AuthService.getAccessForIdentifier(id);
		if (auth != AuthType.ACCESS_FULL)
		    throw new BDRCAPIException(403, NO_ACCESS_ERROR_CODE, "you cannot access this manifest");
		return ManifestService.getManifestForIdentifier(id);
	}

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/collection/{identifier}")
    public Response getCollection(@PathParam("identifier") String identifier) throws BDRCAPIException {

        String output = "Jersey say : " + identifier;
        
        Identifier id = new Identifier(identifier, Identifier.MANIFEST_ID);

        return Response.status(200)
                .entity(output)
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS")
                .build();

    }

}
