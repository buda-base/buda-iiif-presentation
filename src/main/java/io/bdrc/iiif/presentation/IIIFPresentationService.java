package io.bdrc.iiif.presentation;

import static io.bdrc.iiif.presentation.AppConstants.BDR_len;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
import io.bdrc.auth.Access.AccessLevel;
import io.bdrc.auth.AuthProps;
import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.models.AccessType;
import io.bdrc.iiif.presentation.models.ImageInfo;
import io.bdrc.iiif.presentation.models.ItemInfo;
import io.bdrc.iiif.presentation.models.VolumeInfo;
import io.bdrc.iiif.presentation.models.WorkInfo;
import io.bdrc.libraries.Identifier;
import io.bdrc.libraries.IdentifierException;

@Path("/")
public class IIIFPresentationService {

    private static final Logger logger = LoggerFactory.getLogger(IIIFPresentationService.class);
    static final int ACCESS_CONTROL_MAX_AGE_IN_SECONDS = 24 * 60 * 60;

    // no robots on manifests
    @GET
    @Path("/robots.txt")
    public Response getRobots() {
        StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(OutputStream os) throws IOException, WebApplicationException {
                os.write("User-agent: *\nDisallow: /".getBytes());
            }
        };
        return Response.ok(stream, MediaType.TEXT_PLAIN_TYPE).build();
    }

    @POST
    @Path("/clearcache")
    public String clearCache() {
        logger.info("clearing cache >>");
        if (ServiceCache.clearCache()) {
            return "OK";
        } else {
            return "ERROR";
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{version : ([0-9\\.]+/)?}{identifier}/manifest")
    public Response getManifest(@PathParam("identifier") final String identifier, @PathParam("version") final String version, ContainerRequestContext ctx, @Context UriInfo info) throws BDRCAPIException {
        MultivaluedMap<String, String> hm = info.getQueryParameters();
        String cont = hm.getFirst("continuous");
        boolean continuous = false;
        if (cont != null) {
            continuous = Boolean.parseBoolean(cont);
        }
        Identifier id = null;
        try {
            id = new Identifier(identifier, Identifier.MANIFEST_ID);
        } catch (Exception e) {
            e.printStackTrace();
            throw new BDRCAPIException(404, AppConstants.GENERIC_IDENTIFIER_ERROR, e.getMessage());
        }
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
        if (vi.restrictedInChina && GeoLocation.isFromChina(ctx)) {
            return Response.status(403).entity("Insufficient rights").header("Cache-Control", "no-cache").build();
        }
        Access acc = (Access) ctx.getProperty("access");
        if (acc == null)
            acc = new Access();
        final String accessShortName = getShortName(vi.access.getUri());
        final String statusShortName = getShortName(vi.statusUri);
        final AccessLevel al = acc.hasResourceAccess(accessShortName, statusShortName, vi.itemId);
        if (al == AccessLevel.MIXED || al == AccessLevel.NOACCESS) {
            return Response.status(acc.isUserLoggedIn() ? 403 : 401).entity("\"Insufficient rights (" + vi.access + ")\"").header("Cache-Control", "no-cache").build();
        }
        if (vi.iiifManifest != null) {
            logger.info("redirect manifest request for ID {} to {}", identifier, vi.iiifManifest.toString());
            return Response.status(302) // maybe 303 or 307 would be better?
                    .header("Location", vi.iiifManifest).build();
        }
        final Manifest resmanifest = ManifestService.getManifestForIdentifier(id, vi, continuous, wi, volumeId, al == AccessLevel.FAIR_USE);
        final StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(final OutputStream os) throws IOException, WebApplicationException {
                IIIFApiObjectMapperProvider.writer.writeValue(os, resmanifest);
            }
        };
        if (vi.access == AccessType.OPEN) {
            return Response.ok(stream).header("Cache-Control", "public,max-age=" + AuthProps.getProperty("max-age")).build();
        } else {
            return Response.ok(stream).header("Cache-Control", "private,max-age=" + AuthProps.getProperty("max-age")).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{version : ([0-9\\.]+/)?}{identifier}/canvas/{filename}")
    public Response getCanvas(@PathParam("identifier") final String identifier, @PathParam("version") final String version, @PathParam("filename") final String filename, final ContainerRequestContext ctx) throws BDRCAPIException {
        // TODO: adjust to new filename in the path (requires file name lookup in the
        // image list)
        Identifier id = null;
        try {
            id = new Identifier(identifier, Identifier.MANIFEST_ID);
        } catch (IdentifierException e) {
            e.printStackTrace();
            throw new BDRCAPIException(404, AppConstants.GENERIC_IDENTIFIER_ERROR, e.getMessage());
        }
        final VolumeInfo vi = VolumeInfoService.getVolumeInfo(id.getVolumeId(), false); // not entirely sure about the false
        if (vi.restrictedInChina && GeoLocation.isFromChina(ctx)) {
            return Response.status(403).entity("Insufficient rights").header("Cache-Control", "no-cache").build();
        }
        Access acc = (Access) ctx.getProperty("access");
        if (acc == null)
            acc = new Access();
        final boolean logged = acc.isUserLoggedIn();
        final String accessShortName = getShortName(vi.access.getUri());
        final String statusShortName = getShortName(vi.statusUri);
        final AccessLevel al = acc.hasResourceAccess(accessShortName, statusShortName, vi.itemId);
        if (al == AccessLevel.MIXED || al == AccessLevel.NOACCESS) {
            return Response.status(logged ? 403 : 401).entity("\"Insufficient rights (" + vi.access + ")\"").header("Cache-Control", "no-cache").build();
        }
        if (vi.iiifManifest != null) {
            return Response.status(404).entity("\"Cannot serve canvas for external manifests\"").header("Cache-Control", "no-cache").build();
        }
        final List<ImageInfo> imageInfoList = ImageInfoListService.getImageInfoList(vi.workId.substring(BDR_len), vi.imageGroup);
        final Integer imgSeqNum = ManifestService.getFileNameSeqNum(imageInfoList, filename);
        if (imgSeqNum == null)
            throw new BDRCAPIException(500, AppConstants.GENERIC_LDS_ERROR, "Cannot find filename in the S3 image list");
        if (al == AccessLevel.FAIR_USE) {
            if (imgSeqNum > (AppConstants.FAIRUSE_PAGES_S + vi.pagesIntroTbrc) && imgSeqNum < (vi.totalPages - AppConstants.FAIRUSE_PAGES_E))
                return Response.status(logged ? 403 : 401).entity("\"Insufficient rights (" + vi.access + ")\"").header("Cache-Control", "no-cache").build();
        }
        final Canvas res = ManifestService.getCanvasForIdentifier(id, vi, imgSeqNum, id.getVolumeId(), imageInfoList);
        final StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(final OutputStream os) throws IOException, WebApplicationException {
                IIIFApiObjectMapperProvider.writer.writeValue(os, res);
            }
        };
        if (vi.access == AccessType.OPEN) {
            return Response.ok(stream).header("Cache-Control", "public,max-age=" + AuthProps.getProperty("max-age")).build();
        } else {
            return Response.ok(stream).header("Cache-Control", "private,max-age=" + AuthProps.getProperty("max-age")).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{version : ([0-9\\.]+/)?}collection/{identifier}")
    public Response getCollection(@PathParam("identifier") final String identifier, @PathParam("version") final String version, final ContainerRequestContext ctx, @Context UriInfo info) throws BDRCAPIException {
        MultivaluedMap<String, String> hm = info.getQueryParameters();
        String cont = hm.getFirst("continuous");
        boolean continuous = false;
        if (cont != null) {
            continuous = Boolean.parseBoolean(cont);
        }
        Identifier id = null;
        try {
            id = new Identifier(identifier, Identifier.COLLECTION_ID);
        } catch (IdentifierException e) {
            e.printStackTrace();
            throw new BDRCAPIException(404, AppConstants.GENERIC_IDENTIFIER_ERROR, e.getMessage());
        }
        AccessType access = AccessType.RESTR_BDRC;
        boolean restrictedInChina = true;
        final int subType = id.getSubType();
        String itemId = null;
        String statusUri = null;
        boolean isVirtual = false;
        switch (subType) {
        case Identifier.COLLECTION_ID_ITEM:
        case Identifier.COLLECTION_ID_ITEM_VOLUME_OUTLINE:
            final ItemInfo ii = ItemInfoService.getItemInfo(id.getItemId());
            access = ii.access;
            statusUri = ii.statusUri;
            restrictedInChina = ii.restrictedInChina;
            itemId = id.getItemId();
            break;
        case Identifier.COLLECTION_ID_WORK_IN_ITEM:
            final WorkInfo winf = WorkInfoService.getWorkInfo(id.getWorkId());
            access = winf.rootAccess;
            isVirtual = winf.isVirtual;
            statusUri = winf.rootStatus;
            restrictedInChina = winf.rootRestrictedInChina;
            itemId = id.getItemId();
            break;
        case Identifier.COLLECTION_ID_WORK_OUTLINE:
            final WorkInfo winf1 = WorkInfoService.getWorkInfo(id.getWorkId());
            access = winf1.rootAccess;
            isVirtual = winf1.isVirtual;
            statusUri = winf1.rootStatus;
            restrictedInChina = winf1.rootRestrictedInChina;
            itemId = winf1.itemId;
            break;
        }
        if (restrictedInChina && GeoLocation.isFromChina(ctx)) {
            throw new BDRCAPIException(403, AppConstants.GENERIC_LDS_ERROR, "Insufficient rights");
        }
        Access acc = (Access) ctx.getProperty("access");
        if (acc == null)
            acc = new Access();
        final String accessShortName = getShortName(access.getUri());
        final String statusShortName = getShortName(statusUri);
        final AccessLevel al = acc.hasResourceAccess(accessShortName, statusShortName, itemId);
        if (al == AccessLevel.MIXED || al == AccessLevel.NOACCESS) {
            return Response.status(acc.isUserLoggedIn() ? 403 : 401).entity("\"Insufficient rights\"").header("Cache-Control", "no-cache").build();
        }
        int maxAgeSeconds = Integer.parseInt(AuthProps.getProperty("max-age")) / 1000;
        if (access == AccessType.OPEN) {
            return Response.ok().cacheControl(CacheControl.valueOf("public, max-age=" + maxAgeSeconds)).entity(CollectionService.getCollectionForIdentifier(id, continuous)).build();
        } else {
            return Response.ok().cacheControl(CacheControl.valueOf("private, max-age=" + maxAgeSeconds)).entity(CollectionService.getCollectionForIdentifier(id, continuous)).build();
        }
    }

    public static String getShortName(final String st) {
        if (st == null || st.isEmpty()) {
            return null;
        }
        return st.substring(st.lastIndexOf("/") + 1);
    }

}