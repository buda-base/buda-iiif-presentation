package io.bdrc.iiif.presentation;

import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.digitalcollections.iiif.model.sharedcanvas.Canvas;
import de.digitalcollections.iiif.model.sharedcanvas.Manifest;
import io.bdrc.auth.Access;
import io.bdrc.auth.AuthProps;
import io.bdrc.auth.rdf.RdfConstants;
import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.models.AccessType;
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
    public Response getManifest(@PathParam("identifier") final String identifier, ContainerRequestContext ctx, @Context UriInfo info) throws BDRCAPIException {
        boolean isFromChina = ((Boolean) ctx.getProperty("isFromChina")).booleanValue();
        MultivaluedMap<String, String> hm = info.getQueryParameters();
        String cont = hm.getFirst("continuous");
        boolean continuous = false;
        if (cont != null) {
            continuous = Boolean.parseBoolean(cont);
        }
        final Identifier id = new Identifier(identifier, Identifier.MANIFEST_ID);
        boolean requiresVolumeOutline = false;
        if (id.getSubType() == Identifier.MANIFEST_ID_VOLUMEID_OUTLINE) {
            requiresVolumeOutline = true;
        }
        WorkInfo wi = null;
        if (id.getWorkId() != null) {
            wi = WorkInfoService.getWorkInfo(id.getWorkId());
        }
        String volumeId = id.getVolumeId();
        if (volumeId == null) {
            volumeId = wi.firstVolumeId;
            if (volumeId == null)
                return Response.status(404).entity("\"Cannot find volume ID\"").header("Cache-Control", "no-cache").build();
        }
        final VolumeInfo vi = VolumeInfoService.getVolumeInfo(volumeId, requiresVolumeOutline);
        boolean forbidden = vi.restrictedInChina && isFromChina;
        Access acc = (Access) ctx.getProperty("access");
        if (acc == null) {
            acc = new Access();
        }

        if (vi.access == null || !acc.hasResourceAccess(getShortName(vi.access.getUri())) || forbidden) {
            return Response.status(403).entity("\"Insufficient rights (" + vi.access + ")\"").header("Cache-Control", "no-cache").build();
        }
        if (vi.iiifManifest != null) {
            logger.info("redirect manifest request for ID {} to {}", identifier, vi.iiifManifest.toString());
            return Response.status(302) // maybe 303 or 307 would be better?
                    .header("Location", vi.iiifManifest).build();
        }
        final Manifest resmanifest = ManifestService.getManifestForIdentifier(id, vi, continuous, wi, volumeId);
        final StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(final OutputStream os) throws IOException, WebApplicationException {
                IIIFApiObjectMapperProvider.writer.writeValue(os, resmanifest);
            }
        };
        // At this point the resource is accessible but we don't whether it is public or
        // restricted
        // and we don't know either if the user is authenticated or not
        if (vi.access == AccessType.OPEN) {
            return Response.ok(stream).header("Cache-Control", "public,max-age=" + AuthProps.getProperty("max-age")).build();
        } else {
            return Response.ok(stream).header("Cache-Control", "private,max-age=" + AuthProps.getProperty("max-age")).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/2.1.1/{identifier}/canvas/{imgseqnum}")
    public Response getCanvas(@PathParam("identifier") final String identifier, @PathParam("imgseqnum") final String imgseqnum, final ContainerRequestContext ctx) throws BDRCAPIException {
        boolean isFromChina = ((Boolean) ctx.getProperty("isFromChina")).booleanValue();
        final Identifier id = new Identifier(identifier, Identifier.MANIFEST_ID);
        final VolumeInfo vi = VolumeInfoService.getVolumeInfo(id.getVolumeId(), false); // not entirely sure about the false
        boolean forbidden = vi.restrictedInChina && isFromChina;
        Access acc = (Access) ctx.getProperty("access");
        if (acc == null) {
            acc = new Access();
        }
        final String accessType = getShortName(vi.access.getUri());
        if (accessType == null || !acc.hasResourceAccess(accessType) || forbidden) {
            return Response.status(403).entity("Insufficient rights").header("Cache-Control", "no-cache").build();
        }
        if (vi.iiifManifest != null) {
            return Response.status(404).entity("\"Cannot serve canvas for external manifests\"").header("Cache-Control", "no-cache").build();
        }
        final Canvas res = ManifestService.getCanvasForIdentifier(id, vi, imgseqnum, id.getVolumeId());
        final StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(final OutputStream os) throws IOException, WebApplicationException {
                IIIFApiObjectMapperProvider.writer.writeValue(os, res);
            }
        };
        // At this point the resource is accessible but we don't whether it is public or
        // restricted
        // and we don't know either if the user is authenticated or not
        boolean open = accessType.equals(RdfConstants.OPEN);
        if (open) {
            return Response.ok(stream).header("Cache-Control", "public,max-age=" + AuthProps.getProperty("max-age")).build();
        } else {
            return Response.ok(stream).header("Cache-Control", "private,max-age=" + AuthProps.getProperty("max-age")).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/2.1.1/collection/{identifier}")
    public Response getCollection(@PathParam("identifier") final String identifier, final ContainerRequestContext ctx, @Context UriInfo info) throws BDRCAPIException {
        boolean isFromChina = ((Boolean) ctx.getProperty("isFromChina")).booleanValue();
        MultivaluedMap<String, String> hm = info.getQueryParameters();
        String cont = hm.getFirst("continuous");
        boolean continuous = false;
        if (cont != null) {
            continuous = Boolean.parseBoolean(cont);
        }
        final Identifier id = new Identifier(identifier, Identifier.COLLECTION_ID);
        final VolumeInfo vi = VolumeInfoService.getVolumeInfo(id.getVolumeId(), false);
        boolean forbidden = vi.restrictedInChina && isFromChina;
        AccessType access = AccessType.RESTR_BDRC;
        final int subType = id.getSubType();
        switch (subType) {
        case Identifier.COLLECTION_ID_ITEM:
        case Identifier.COLLECTION_ID_ITEM_VOLUME_OUTLINE:
            access = ItemInfoService.getItemInfo(id.getItemId()).access;
            break;
        case Identifier.COLLECTION_ID_WORK_IN_ITEM:
            WorkInfo winf = WorkInfoService.getWorkInfo(id.getWorkId());
            access = winf.rootAccess;
            break;
        case Identifier.COLLECTION_ID_WORK_OUTLINE:
            WorkInfo winf1 = WorkInfoService.getWorkInfo(id.getWorkId());
            access = winf1.rootAccess;
            break;
        }
        Access acc = (Access) ctx.getProperty("access");
        if (acc == null) {
            acc = new Access();
        }
        if (forbidden) {
            throw new BDRCAPIException(403, AppConstants.GENERIC_LDS_ERROR, "Insufficient rights");
        }
        // At this point the resource is accessible but we don't whether it is public or
        // restricted
        // and we don't know either if the user is authenticated or not
        int maxAgeSeconds = Integer.parseInt(AuthProps.getProperty("max-age")) / 1000;
        if (access == AccessType.OPEN) {
            return Response.ok().cacheControl(CacheControl.valueOf("public, max-age=" + maxAgeSeconds)).entity(CollectionService.getCollectionForIdentifier(id, continuous)).build();
        } else {
            return Response.ok().cacheControl(CacheControl.valueOf("private, max-age=" + maxAgeSeconds)).entity(CollectionService.getCollectionForIdentifier(id, continuous)).build();
        }
    }

    public static String getShortName(final String st) {
        return st.substring(st.lastIndexOf("/") + 1);
    }

}