package io.bdrc.iiif.presentation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.digitalcollections.iiif.model.sharedcanvas.Canvas;
import de.digitalcollections.iiif.model.sharedcanvas.Collection;
import de.digitalcollections.iiif.model.sharedcanvas.Manifest;
import io.bdrc.auth.Access;
import io.bdrc.auth.Access.AccessLevel;
import io.bdrc.auth.AuthProps;
import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.resmodels.AccessType;
import io.bdrc.iiif.presentation.resmodels.ImageInfo;
import io.bdrc.iiif.presentation.resmodels.ItemInfo;
import io.bdrc.iiif.presentation.resmodels.PartInfo;
import io.bdrc.iiif.presentation.resmodels.VolumeInfo;
import io.bdrc.iiif.presentation.resmodels.WorkInfo;
import io.bdrc.iiif.presentation.resmodels.WorkOutline;
import io.bdrc.iiif.presentation.resservices.ImageInfoListService;
import io.bdrc.iiif.presentation.resservices.ItemInfoService;
import io.bdrc.iiif.presentation.resservices.ServiceCache;
import io.bdrc.iiif.presentation.resservices.VolumeInfoService;
import io.bdrc.iiif.presentation.resservices.WorkInfoService;
import io.bdrc.iiif.presentation.resservices.WorkOutlineService;
import io.bdrc.libraries.GitHelpers;
import io.bdrc.libraries.GlobalHelpers;
import io.bdrc.libraries.Identifier;
import io.bdrc.libraries.IdentifierException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;

@RestController
@RequestMapping("/")
public class IIIFPresentationService {

    private static final Logger logger = LoggerFactory.getLogger(IIIFPresentationService.class);
    static final int ACCESS_CONTROL_MAX_AGE_IN_SECONDS = 24 * 60 * 60;

    final static ObjectMapper om = new ObjectMapper();

    @Autowired
    MeterRegistry registry;

    // no robots on manifests
    @RequestMapping(value = "/robots.txt", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getRobots(HttpServletResponse response) {
        return ResponseEntity.ok("User-agent: *\nDisallow: /");
    }

    @RequestMapping(value = "/clearcache", method = RequestMethod.POST, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> clearCache() {
        logger.info("clearing cache >>");
        if (ServiceCache.clearCache()) {
            return ResponseEntity.ok("OK");
        } else {
            return ResponseEntity.ok("ERROR");
        }
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/admin/cache/{region}/{key}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Object> getKey(@PathVariable String region, @PathVariable final String key) throws BDRCAPIException {
        if (!AppConstants.CACHENAME.equals(region))
            throw new BDRCAPIException(404, AppConstants.GENERIC_IDENTIFIER_ERROR, "unknown region");
        if (key.length() < 4)
            throw new BDRCAPIException(404, AppConstants.GENERIC_IDENTIFIER_ERROR, "key is too short");
        final String prefix = key.substring(0, 3);
        final Object res = ServiceCache.getObjectFromCache(key);
        if (res == null)
            throw new BDRCAPIException(404, AppConstants.GENERIC_IDENTIFIER_ERROR, "key not found");
        // this is quite repetitive unfortunately but I couldn't find another way...
        switch (prefix) {
        case AppConstants.CACHEPREFIX_WI:
            return ResponseEntity.ok().body((WorkInfo) res);
        case AppConstants.CACHEPREFIX_WO:
            return ResponseEntity.ok().body((WorkOutline) res);
        case AppConstants.CACHEPREFIX_II:
            return ResponseEntity.ok().body((ItemInfo) res);
        case AppConstants.CACHEPREFIX_IIL:
            return ResponseEntity.ok().body((List<ImageInfo>) res);
        case AppConstants.CACHEPREFIX_VI:
            return ResponseEntity.ok().body((VolumeInfo) res);
        default:
            throw new BDRCAPIException(500, AppConstants.GENERIC_IDENTIFIER_ERROR, "unhandled key prefix, this shouldn't happen");
        }
    }

    @RequestMapping(value = "/collection/{identifier}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<StreamingResponseBody> getCollectionNoVer(@PathVariable String identifier, HttpServletRequest request,
            HttpServletResponse resp) throws BDRCAPIException {
        return getCollection(identifier, "", request, resp);
    }

    @RequestMapping(value = "/{version:.+}/collection/{identifier}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<StreamingResponseBody> getCollection(@PathVariable String identifier, @PathVariable String version,
            HttpServletRequest request, HttpServletResponse resp) throws BDRCAPIException {
        resp.setContentType("application/json;charset=UTF-8");
        String cont = request.getParameter("continuous");
        boolean continuous = false;
        if (cont != null) {
            continuous = Boolean.parseBoolean(cont);
        }
        Identifier id = null;
        try {
            id = new Identifier(identifier, Identifier.COLLECTION_ID);
        } catch (IdentifierException e) {
            e.printStackTrace();
            Metrics.counter("exit.status", "result", "404").increment();
            throw new BDRCAPIException(404, AppConstants.GENERIC_IDENTIFIER_ERROR, e.getMessage());
        }
        AccessType access = AccessType.RESTR_BDRC;
        boolean restrictedInChina = true;
        final int subType = id.getSubType();
        String itemId = null;
        String statusUri = null;
        switch (subType) {
        case Identifier.COLLECTION_ID_ITEM:
        case Identifier.COLLECTION_ID_ITEM_VOLUME_OUTLINE:
            ItemInfo ii;
            try {
                ii = ItemInfoService.Instance.getAsync(id.getItemId()).get();
            } catch (InterruptedException | ExecutionException e1) {
                Metrics.counter("exit.status", "result", "500").increment();
                throw new BDRCAPIException(500, AppConstants.GENERIC_IDENTIFIER_ERROR, e1);
            }
            access = ii.access;
            statusUri = ii.statusUri;
            restrictedInChina = ii.restrictedInChina;
            itemId = id.getItemId();
            break;
        case Identifier.COLLECTION_ID_WORK_IN_ITEM:
            WorkInfo winf;
            try {
                winf = WorkInfoService.Instance.getAsync(id.getWorkId()).get();
            } catch (InterruptedException | ExecutionException e) {
                Metrics.counter("exit.status", "result", "500").increment();
                throw new BDRCAPIException(500, AppConstants.GENERIC_IDENTIFIER_ERROR, e);
            }
            access = winf.rootAccess;
            statusUri = winf.rootStatus;
            restrictedInChina = winf.rootRestrictedInChina;
            itemId = id.getItemId();
            break;
        case Identifier.COLLECTION_ID_WORK_OUTLINE:
            WorkInfo winf1;
            try {
                winf1 = WorkInfoService.Instance.getAsync(id.getWorkId()).get();
            } catch (InterruptedException | ExecutionException e2) {
                Metrics.counter("exit.status", "result", "500").increment();
                throw new BDRCAPIException(500, AppConstants.GENERIC_IDENTIFIER_ERROR, e2);
            }
            access = winf1.rootAccess;
            statusUri = winf1.rootStatus;
            restrictedInChina = winf1.rootRestrictedInChina;
            itemId = winf1.itemId;
            break;
        }
        if (restrictedInChina && GeoLocation.isFromChina(request)) {
            Metrics.counter("exit.status", "result", "403").increment();
            throw new BDRCAPIException(403, AppConstants.GENERIC_LDS_ERROR, "Insufficient rights");
        }
        Access acc = (Access) request.getAttribute("access");
        if (acc == null)
            acc = new Access();
        final String accessShortName = getShortName(access.getUri());
        final String statusShortName = getShortName(statusUri);
        final AccessLevel al = acc.hasResourceAccess(accessShortName, statusShortName, itemId);
        if (al == AccessLevel.MIXED || al == AccessLevel.NOACCESS) {
            Metrics.counter("exit.status", "result", acc.isUserLoggedIn() ? "403" : "401").increment();
            return ResponseEntity.status(acc.isUserLoggedIn() ? 403 : 401).cacheControl(CacheControl.noCache())
                    .body(getStream("Insufficient rights"));
        }
        int maxAgeSeconds = Integer.parseInt(AuthProps.getProperty("max-age")) / 1000;
        Collection coll = CollectionService.getCollectionForIdentifier(id, continuous);
        if (access == AccessType.OPEN) {
            return ResponseEntity.ok().cacheControl(CacheControl.maxAge(maxAgeSeconds, TimeUnit.SECONDS).cachePublic()).body(getStream(coll));
        } else {
            return ResponseEntity.ok().cacheControl(CacheControl.maxAge(maxAgeSeconds, TimeUnit.SECONDS).cachePrivate()).body(getStream(coll));
        }
    }

    @RequestMapping(value = "/{identifier}/manifest", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<StreamingResponseBody> getManifestNoVer(@PathVariable String identifier, HttpServletRequest req, HttpServletResponse resp)
            throws BDRCAPIException, JsonGenerationException, JsonMappingException, IOException {
        return getManifest(identifier, "", req, resp);
    }

    @RequestMapping(value = "/{version:.+}/{identifier}/manifest", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<StreamingResponseBody> getManifest(@PathVariable String identifier, @PathVariable String version, HttpServletRequest req,
            HttpServletResponse resp) throws BDRCAPIException, JsonGenerationException, JsonMappingException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String cont = req.getParameter("continuous");
        boolean continuous = false;
        if (cont != null) {
            continuous = Boolean.parseBoolean(cont);
        }
        Identifier id = null;
        try {
            id = new Identifier(identifier, Identifier.MANIFEST_ID);
        } catch (Exception e) {
            throw new BDRCAPIException(404, AppConstants.GENERIC_IDENTIFIER_ERROR, e);
        }
        PartInfo rootPart = null;
        WorkInfo wi = null;
        if (id.getWorkId() != null
                && (id.getSubType() == Identifier.MANIFEST_ID_WORK_IN_ITEM || id.getSubType() == Identifier.MANIFEST_ID_WORK_IN_VOLUMEID)) {
            try {
                wi = WorkInfoService.Instance.getAsync(id.getWorkId()).get();
                rootPart = wi;
                logger.debug("for {} got workInfo {}", identifier, wi);
            } catch (InterruptedException | ExecutionException e) {
                throw new BDRCAPIException(500, AppConstants.GENERIC_IDENTIFIER_ERROR, e);
            }
        }
        WorkOutline wo = null;
        // quite specific: we want to compute wo here only if we don't have the volume
        // id
        if (id.getSubType() == Identifier.MANIFEST_ID_WORK_IN_VOLUMEID_OUTLINE && id.getWorkId() != null && id.getVolumeId() == null) {
            try {
                wo = WorkOutlineService.Instance.getAsync(id.getWorkId()).get();
                rootPart = wo.getPartForWorkId(id.getWorkId());
            } catch (InterruptedException | ExecutionException e) {
                throw new BDRCAPIException(500, AppConstants.GENERIC_IDENTIFIER_ERROR, e);
            }
        }
        String volumeId = id.getVolumeId();
        if (volumeId == null) {
            if (wi != null)
                volumeId = wi.firstVolumeId;
            if (wo != null)
                volumeId = wo.firstVolumeId;
            if (volumeId == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(CacheControl.noCache()).body(getStream("\"Cannot find volume ID\""));
            }
        }
        VolumeInfo vi;
        try {
            vi = VolumeInfoService.Instance.getAsync(volumeId).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new BDRCAPIException(500, AppConstants.GENERIC_IDENTIFIER_ERROR, e);
        }
        if (vi.restrictedInChina && GeoLocation.isFromChina(req)) {
            return ResponseEntity.status(HttpStatus.resolve(403)).cacheControl(CacheControl.noCache()).body(getStream("Insufficient rights"));
        }
        Access acc = (Access) req.getAttribute("access");
        if (acc == null)
            acc = new Access();
        final String accessShortName = getShortName(vi.access.getUri());
        final String statusShortName = getShortName(vi.statusUri);
        final AccessLevel al = acc.hasResourceAccess(accessShortName, statusShortName, vi.itemId);
        if (al == AccessLevel.MIXED || al == AccessLevel.NOACCESS) {
            return ResponseEntity.status(HttpStatus.resolve(acc.isUserLoggedIn() ? 403 : 401)).cacheControl(CacheControl.noCache())
                    .body(getStream("Insufficient rights"));
        }
        if (vi.iiifManifest != null) {
            logger.info("redirect manifest request for ID {} to {}", identifier, vi.iiifManifest.toString());
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.add("Location", vi.iiifManifest.toString());
            return new ResponseEntity<StreamingResponseBody>(responseHeaders, HttpStatus.resolve(302));
        }
        if (wo == null && (id.getSubType() == Identifier.MANIFEST_ID_VOLUMEID_OUTLINE
                || id.getSubType() == Identifier.MANIFEST_ID_WORK_IN_VOLUMEID_OUTLINE)) {
            try {
                // important: we take the outline of the whole root work, that makes
                // caching more efficient
                wo = WorkOutlineService.Instance.getAsync(vi.workId).get();
                String shortWorkId = id.getWorkId();
                if (shortWorkId == null)
                    shortWorkId = "bdr:" + vi.workId.substring(AppConstants.BDR_len);
                rootPart = wo.getPartForWorkId(shortWorkId);
                // TODO: case of a virtual work pointing to an outline
                // we should copy the labels of the virtual work...
            } catch (InterruptedException | ExecutionException e) {
                throw new BDRCAPIException(500, AppConstants.GENERIC_IDENTIFIER_ERROR, e);
            }
        }
        if (rootPart == null) {
            // case of a virtual work with a location
            if (wi == null && id.getWorkId() != null) {
                try {
                    wi = WorkInfoService.Instance.getAsync(id.getWorkId()).get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new BDRCAPIException(500, AppConstants.GENERIC_IDENTIFIER_ERROR, e);
                }
            }
            rootPart = wi;
        }
        // TODO: case where a part is asked with an outline, we need to make sure that
        // we get the part asked by the user
        final Manifest resmanifest = ManifestService.getManifestForIdentifier(id, vi, continuous, volumeId, al == AccessLevel.FAIR_USE, rootPart);
        if (vi.access == AccessType.OPEN) {
            return ResponseEntity.status(HttpStatus.OK)
                    .cacheControl(CacheControl.maxAge(Long.parseLong(AuthProps.getProperty("max-age")), TimeUnit.SECONDS).cachePublic())
                    .body(getStream(resmanifest));
        } else {
            return ResponseEntity.status(HttpStatus.OK)
                    .cacheControl(CacheControl.maxAge(Long.parseLong(AuthProps.getProperty("max-age")), TimeUnit.SECONDS).cachePrivate())
                    .body(getStream(resmanifest));
        }
    }

    @RequestMapping(value = "/{identifier}/canvas/{filename}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<StreamingResponseBody> getCanvasNoVer(@PathVariable String identifier, @PathVariable String filename,
            HttpServletRequest req, HttpServletResponse resp) throws BDRCAPIException, JsonGenerationException, JsonMappingException, IOException {
        return getCanvas(identifier, "", filename, req, resp);
    }

    @RequestMapping(value = "/{version:.+}/{identifier}/canvas/{filename}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<StreamingResponseBody> getCanvas(@PathVariable String identifier, @PathVariable String version,
            @PathVariable String filename, HttpServletRequest req, HttpServletResponse resp)
            throws BDRCAPIException, JsonGenerationException, JsonMappingException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        Identifier id = null;
        try {
            id = new Identifier(identifier, Identifier.MANIFEST_ID);
        } catch (IdentifierException e) {
            throw new BDRCAPIException(404, AppConstants.GENERIC_IDENTIFIER_ERROR, e);
        }
        VolumeInfo vi;
        try {
            vi = VolumeInfoService.Instance.getAsync(id.getVolumeId()).get();
        } catch (InterruptedException | ExecutionException e1) {
            throw new BDRCAPIException(500, AppConstants.GENERIC_IDENTIFIER_ERROR, e1);
        } // not entirely sure about the false
        if (vi.restrictedInChina && GeoLocation.isFromChina(req)) {
            return ResponseEntity.status(HttpStatus.resolve(403)).cacheControl(CacheControl.noCache()).body(getStream("Insufficient rights"));
        }
        Access acc = (Access) req.getAttribute("access");
        if (acc == null)
            acc = new Access();
        final String accessShortName = getShortName(vi.access.getUri());
        final String statusShortName = getShortName(vi.statusUri);
        final AccessLevel al = acc.hasResourceAccess(accessShortName, statusShortName, vi.itemId);
        if (al == AccessLevel.MIXED || al == AccessLevel.NOACCESS) {
            return ResponseEntity.status(HttpStatus.resolve(acc.isUserLoggedIn() ? 403 : 401)).cacheControl(CacheControl.noCache())
                    .body(getStream("Insufficient rights"));
        }
        if (vi.iiifManifest != null) {
            return ResponseEntity.status(HttpStatus.resolve(404)).cacheControl(CacheControl.noCache())
                    .body(getStream("\"Cannot serve canvas for external manifests\""));
        }
        List<ImageInfo> imageInfoList;
        try {
            imageInfoList = ImageInfoListService.Instance.getAsync(vi.workId.substring(AppConstants.BDR_len), vi.imageGroup).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new BDRCAPIException(500, AppConstants.GENERIC_IDENTIFIER_ERROR, e);
        }
        final Integer imgSeqNum = ManifestService.getFileNameSeqNum(imageInfoList, filename);
        if (imgSeqNum == null)
            throw new BDRCAPIException(500, AppConstants.GENERIC_LDS_ERROR, "Cannot find filename in the S3 image list");
        if (al == AccessLevel.FAIR_USE) {
            if (imgSeqNum > (AppConstants.FAIRUSE_PAGES_S + vi.pagesIntroTbrc) && imgSeqNum < (vi.totalPages - AppConstants.FAIRUSE_PAGES_E)) {
                return ResponseEntity.status(HttpStatus.resolve(acc.isUserLoggedIn() ? 403 : 401)).cacheControl(CacheControl.noCache())
                        .body(getStream("\"Insufficient rights (" + vi.access + ")\""));
            }
        }
        final Canvas res = ManifestService.getCanvasForIdentifier(id, vi, imgSeqNum, id.getVolumeId(), imageInfoList);
        if (vi.access == AccessType.OPEN) {
            return ResponseEntity.status(HttpStatus.OK)
                    .cacheControl(CacheControl.maxAge(Long.parseLong(AuthProps.getProperty("max-age")), TimeUnit.SECONDS).cachePublic())
                    .body(getStream(res));
        } else {
            return ResponseEntity.status(HttpStatus.OK)
                    .cacheControl(CacheControl.maxAge(Long.parseLong(AuthProps.getProperty("max-age")), TimeUnit.SECONDS).cachePrivate())
                    .body(getStream(res));
        }
    }

    @RequestMapping(value = "/il/v:{volumeId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Object> getImageList(@PathVariable String volumeId, HttpServletRequest request, HttpServletResponse resp)
            throws BDRCAPIException {
        resp.setContentType("application/json;charset=UTF-8");
        VolumeInfo vi;
        try {
            vi = VolumeInfoService.Instance.getAsync(volumeId).get();
        } catch (InterruptedException | ExecutionException e1) {
            throw new BDRCAPIException(404, AppConstants.GENERIC_IDENTIFIER_ERROR, e1);
        }
        List<ImageInfo> imageInfoList;
        try {
            imageInfoList = ImageInfoListService.Instance.getAsync(vi.workId.substring(AppConstants.BDR_len), vi.imageGroup).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new BDRCAPIException(404, AppConstants.GENERIC_IDENTIFIER_ERROR, e);
        }
        return ResponseEntity.status(HttpStatus.OK)
                .cacheControl(CacheControl.maxAge(Long.parseLong(AuthProps.getProperty("max-age")), TimeUnit.SECONDS).cachePublic())
                .body(imageInfoList);
    }

    @RequestMapping(value = "/bvm/{resource}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<String> getImageInfoFile(@PathVariable String resource, HttpServletRequest request, HttpServletResponse resp)
            throws BDRCAPIException, IOException {
        String json = "";
        try {
            String res = resource.substring(resource.indexOf(":") + 1);
            System.out.println("RES >>> " + res);
            String filename = System.getProperty("iiifpres.configpath") + "gitData/buda-volume-manifests/" + GlobalHelpers.getTwoLettersBucket(res)
                    + "/" + res + ".json";
            json = GlobalHelpers.readFileContent(filename);
        } catch (Exception e) {
            throw new BDRCAPIException(404, AppConstants.GENERIC_APP_ERROR_CODE, e);
        }
        return ResponseEntity.status(HttpStatus.OK)
                .cacheControl(CacheControl.maxAge(Long.parseLong(AuthProps.getProperty("max-age")), TimeUnit.SECONDS).cachePublic()).body(json);
    }

    @RequestMapping(value = "/bvm/{resource}", method = RequestMethod.PUT)
    public ResponseEntity<String> writeImageInfoFile(@PathVariable String resource, @RequestBody String json, HttpServletRequest request,
            HttpServletResponse resp) throws BDRCAPIException, NoSuchAlgorithmException {
        try {
            String repoBase = System.getProperty("iiifpres.configpath") + "gitData/buda-volume-manifests/";
            String res = resource.substring(resource.indexOf(":") + 1);
            String filename = repoBase + GlobalHelpers.getTwoLettersBucket(res) + "/" + res + ".json";
            File f = new File(repoBase + GlobalHelpers.getTwoLettersBucket(res) + "/");
            if (!f.exists()) {
                f.mkdir();
            }
            BufferedWriter br = new BufferedWriter(new FileWriter(new File(filename)));
            br.write(json);
            br.close();
            Repository repo = GitHelpers.ensureGitRepo(repoBase);
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            GitHelpers.commitChanges(repo, "Added/updated " + res + ".json on " + dtf.format(LocalDateTime.now()));
            GitHelpers.push(repo, AuthProps.getProperty("gitRemoteUrl"), AuthProps.getProperty("gitUser"), AuthProps.getProperty("gitPass"));
        } catch (Exception e) {
            throw new BDRCAPIException(404, AppConstants.GENERIC_APP_ERROR_CODE, e);
        }
        return ResponseEntity.status(HttpStatus.OK)
                .cacheControl(CacheControl.maxAge(Long.parseLong(AuthProps.getProperty("max-age")), TimeUnit.SECONDS).cachePublic()).body(json);
    }

    public static String getShortName(final String st) {
        if (st == null || st.isEmpty()) {
            return null;
        }
        return st.substring(st.lastIndexOf("/") + 1);
    }

    private StreamingResponseBody getStream(Object obj) {
        final StreamingResponseBody stream = new StreamingResponseBody() {
            @Override
            public void writeTo(final OutputStream os) throws IOException {
                AppConstants.IIIFMAPPER.writer().writeValue(os, obj);
            }
        };
        return stream;
    }
}
