package io.bdrc.iiif.presentation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;

@Path("/2")
public class IIIFPresentationService {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{identifier}/manifest")
	public Response getManifest(@PathParam("identifier") String identifier) throws BDRCAPIException {

		String output = "Jersey say : " + identifier;

		return Response.status(200).entity(output).build();

	}

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/collection/{identifier}")
    public Response getCollection(@PathParam("identifier") String identifier) throws BDRCAPIException {

        String output = "Jersey say : " + identifier;

        return Response.status(200).entity(output).build();

    }

}
