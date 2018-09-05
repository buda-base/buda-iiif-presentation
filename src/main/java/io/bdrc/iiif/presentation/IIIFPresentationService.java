package io.bdrc.iiif.presentation;

import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.digitalcollections.iiif.model.sharedcanvas.Collection;
import de.digitalcollections.iiif.model.sharedcanvas.Manifest;
import io.bdrc.auth.Access;
import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.models.Identifier;
import io.bdrc.iiif.presentation.models.VolumeInfo;
import io.bdrc.iiif.presentation.models.WorkInfo;

@Path("/")
public class IIIFPresentationService {

    private static final Logger logger = LoggerFactory.getLogger(IIIFPresentationService.class);
    static final int ACCESS_CONTROL_MAX_AGE_IN_SECONDS = 24 * 60 * 60;

    @GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/2.1.1/{identifier}/manifest")
	@JerseyCacheControl()
	// add @Context UriInfo uriInfo to the arguments to get auth header
	public Response getManifest(@PathParam("identifier") final String identifier, ContainerRequestContext ctx) throws BDRCAPIException {
		final Identifier id = new Identifier(identifier, Identifier.MANIFEST_ID);
		final VolumeInfo vi = VolumeInfoService.getVolumeInfo(id.getVolumeId());
		Access acc=(Access)ctx.getProperty("access");
	    String accessType=getShortName(vi.access.getUri());
	    if(accessType==null || !acc.hasResourceAccess(accessType)) {
	        return Response.status(403).entity("Insufficient rights").build();
	    }
		if (vi.iiifManifest != null) {
		    logger.info("redirect manifest request for ID {} to {}", identifier, vi.iiifManifest.toString());
	        return Response.status(302) // maybe 303 or 307 would be better?
	                .header("Location", vi.iiifManifest)
	                .build();
		}
		final Manifest resmanifest = ManifestService.getManifestForIdentifier(id, vi);
        final StreamingOutput stream = new StreamingOutput() {
            @Override
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
    public Collection getCollection(@PathParam("identifier") final String identifier,ContainerRequestContext ctx) throws BDRCAPIException {
        final Identifier id = new Identifier(identifier, Identifier.COLLECTION_ID);
        String accessType="";
        final int subType=id.getSubType();
        switch(subType) {
            case Identifier.COLLECTION_ID_ITEM:
                accessType=getShortName(ItemInfoService.getItemInfo(id.getItemId()).access.getUri());
                break;
            case Identifier.COLLECTION_ID_WORK_IN_ITEM:
                WorkInfo winf=WorkInfoService.getWorkInfo(id.getWorkId());
                accessType=getShortName(winf.rootAccess);
                break;
            case Identifier.COLLECTION_ID_WORK_OUTLINE:
                WorkInfo winf1=WorkInfoService.getWorkInfo(id.getWorkId());
                accessType=getShortName(winf1.rootAccess);
                break;
        }
        Access acc=(Access)ctx.getProperty("access");
        if(!acc.hasResourceAccess(accessType)) {
            throw new BDRCAPIException(403, AppConstants.GENERIC_LDS_ERROR, "Insufficient rights");
        }
        return CollectionService.getCollectionForIdentifier(id);
    }

    public static String getShortName(String st) {
        return st.substring(st.lastIndexOf("/")+1);
    }

}
