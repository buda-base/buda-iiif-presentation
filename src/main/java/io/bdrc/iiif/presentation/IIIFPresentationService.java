package io.bdrc.iiif.presentation;

import static io.bdrc.iiif.presentation.AppConstants.GENERIC_APP_ERROR_CODE;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Hex;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.digitalcollections.iiif.model.sharedcanvas.Canvas;
import de.digitalcollections.iiif.model.sharedcanvas.Collection;
import de.digitalcollections.iiif.model.sharedcanvas.Manifest;
import io.bdrc.auth.Access;
import io.bdrc.auth.Access.AccessLevel;
import io.bdrc.auth.AuthProps;
import io.bdrc.auth.model.User;
import io.bdrc.auth.rdf.RdfAuthModel;
import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.resmodels.AccessType;
import io.bdrc.iiif.presentation.resmodels.BVM;
import io.bdrc.iiif.presentation.resmodels.BVM.ChangeLogItem;
import io.bdrc.iiif.presentation.resmodels.ImageGroupInfo;
import io.bdrc.iiif.presentation.resmodels.ImageInfoList;
import io.bdrc.iiif.presentation.resmodels.ImageInstanceInfo;
import io.bdrc.iiif.presentation.resmodels.InstanceInfo;
import io.bdrc.iiif.presentation.resmodels.InstanceOutline;
import io.bdrc.iiif.presentation.resmodels.PartInfo;
import io.bdrc.iiif.presentation.resservices.BVMService;
import io.bdrc.iiif.presentation.resservices.ImageGroupInfoService;
import io.bdrc.iiif.presentation.resservices.ImageInfoListService;
import io.bdrc.iiif.presentation.resservices.ImageInstanceInfoService;
import io.bdrc.iiif.presentation.resservices.InstanceInfoService;
import io.bdrc.iiif.presentation.resservices.InstanceOutlineService;
import io.bdrc.iiif.presentation.resservices.ServiceCache;
import io.bdrc.libraries.GitHelpers;
import io.bdrc.libraries.IdentifierException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;

@RestController
@RequestMapping("/")
public class IIIFPresentationService {

    private static final Logger logger = LoggerFactory.getLogger(IIIFPresentationService.class);
    static final int ACCESS_CONTROL_MAX_AGE_IN_SECONDS = 24 * 60 * 60;

    @Autowired
    MeterRegistry registry;

    // no robots on manifests
    @RequestMapping(value = "/robots.txt", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getRobots(HttpServletResponse response) {
        return ResponseEntity.ok("User-agent: *\nDisallow: /");
    }

    @RequestMapping(value = "/clearcache", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<String> clearCache() throws BDRCAPIException {
        logger.info("clearing cache >>");
        BVMService.pullIfNecessary();
        BVMService.pushWhenNecessary();
        if (ServiceCache.clearCache()) {
            return ResponseEntity.ok("\"OK\"");
        } else {
            return ResponseEntity.ok("\"ERROR\"");
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
            return ResponseEntity.ok().body((InstanceInfo) res);
        case AppConstants.CACHEPREFIX_WO:
            return ResponseEntity.ok().body((InstanceOutline) res);
        case AppConstants.CACHEPREFIX_II:
            return ResponseEntity.ok().body((ImageInstanceInfo) res);
        case AppConstants.CACHEPREFIX_IIL:
            return ResponseEntity.ok().body(((ImageInfoList) res).list);
        case AppConstants.CACHEPREFIX_VI:
            return ResponseEntity.ok().body((ImageGroupInfo) res);
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
            ImageInstanceInfo iiInf;
            try {
                iiInf = ImageInstanceInfoService.Instance.getAsync(id.getImageInstanceId()).get();
            } catch (InterruptedException | ExecutionException e1) {
                Metrics.counter("exit.status", "result", "404").increment();
                throw new BDRCAPIException(404, AppConstants.GENERIC_IDENTIFIER_ERROR, e1);
            }
            access = iiInf.access;
            statusUri = iiInf.statusUri;
            restrictedInChina = iiInf.restrictedInChina;
            itemId = id.getImageInstanceId();
            break;
        case Identifier.COLLECTION_ID_WORK_IN_ITEM:
            InstanceInfo winf;
            try {
                winf = InstanceInfoService.Instance.getAsync(id.getInstanceId()).get();
            } catch (InterruptedException | ExecutionException e) {
                Metrics.counter("exit.status", "result", "404").increment();
                throw new BDRCAPIException(404, AppConstants.GENERIC_IDENTIFIER_ERROR, e);
            }
            access = winf.rootAccess;
            statusUri = winf.rootStatusUri;
            restrictedInChina = winf.rootRestrictedInChina;
            itemId = id.getImageInstanceId();
            break;
        case Identifier.COLLECTION_ID_WORK_OUTLINE:
            InstanceInfo winf1;
            try {
                winf1 = InstanceInfoService.Instance.getAsync(id.getInstanceId()).get();
            } catch (InterruptedException | ExecutionException e2) {
                Metrics.counter("exit.status", "result", "404").increment();
                throw new BDRCAPIException(404, AppConstants.GENERIC_IDENTIFIER_ERROR, e2);
            }
            access = winf1.rootAccess;
            statusUri = winf1.rootStatusUri;
            restrictedInChina = winf1.rootRestrictedInChina;
            itemId = winf1.imageInstanceQname;
            break;
        }
        if (restrictedInChina && GeoLocation.isFromChina(request)) {
            Metrics.counter("exit.status", "result", "403").increment();
            throw new BDRCAPIException(403, AppConstants.GENERIC_LDS_ERROR, "Insufficient rights");
        }
        Access acc = (Access) request.getAttribute("access");
        if (acc == null)
            acc = new Access();
        final String accessShortName = getLocalName(access.getUri());
        final String statusShortName = getLocalName(statusUri);
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
            throws BDRCAPIException {
        return getManifest(identifier, "", req, resp);
    }

    @RequestMapping(value = "/{version:.+}/{identifier}/manifest", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<StreamingResponseBody> getManifest(@PathVariable String identifier, @PathVariable String version, HttpServletRequest req,
            HttpServletResponse resp) throws BDRCAPIException {
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
        InstanceInfo iInf = null;
        if (id.getInstanceId() != null
                && (id.getSubType() == Identifier.MANIFEST_ID_WORK_IN_ITEM || id.getSubType() == Identifier.MANIFEST_ID_WORK_IN_VOLUMEID)) {
            try {
                iInf = InstanceInfoService.Instance.getAsync(id.getInstanceId()).get();
                rootPart = iInf;
                logger.debug("for {} got workInfo {}", identifier, iInf);
            } catch (InterruptedException | ExecutionException e) {
                throw new BDRCAPIException(500, AppConstants.GENERIC_IDENTIFIER_ERROR, e);
            }
        }
        InstanceOutline wo = null;
        // quite specific: we want to compute wo here only if we don't have the volume
        // id
        logger.debug("for {} got ID {}", identifier, id);
        if (id.getSubType() == Identifier.MANIFEST_ID_WORK_IN_VOLUMEID_OUTLINE && id.getInstanceId() != null && id.getImageGroupId() == null) {
            try {
                wo = InstanceOutlineService.Instance.getAsync(id.getInstanceId()).get();
                rootPart = wo.getPartForInstanceId(id.getInstanceId());
            } catch (InterruptedException | ExecutionException e) {
                throw new BDRCAPIException(500, AppConstants.GENERIC_IDENTIFIER_ERROR, e);
            }
        }
        logger.debug("for {} got InstanceOutline {}", identifier, wo);
        String volumeId = id.getImageGroupId();
        if (volumeId == null) {
            if (iInf != null)
                volumeId = iInf.firstImageGroupQname;
            if (wo != null)
                volumeId = wo.firstImageGroupQname;
            if (volumeId == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(CacheControl.noCache()).body(getStream("\"Cannot find volume ID\""));
            }
        }
        ImageGroupInfo vi;
        try {
            vi = ImageGroupInfoService.Instance.getAsync(volumeId).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new BDRCAPIException(500, AppConstants.GENERIC_IDENTIFIER_ERROR, e);
        }
        logger.debug("for {} got ImageGroupInfo {}", identifier, vi);
        if (vi.restrictedInChina && GeoLocation.isFromChina(req)) {
            return ResponseEntity.status(HttpStatus.resolve(403)).cacheControl(CacheControl.noCache()).body(getStream("Insufficient rights"));
        }
        Access acc = (Access) req.getAttribute("access");
        boolean isAdmin = false;
        if (acc == null)
            acc = new Access();
        User usr = acc.getUser();
        isAdmin = usr.isAdmin();
        final String accessShortName = getLocalName(vi.access.getUri());
        final String statusShortName = getLocalName(vi.statusUri);
        final AccessLevel al = acc.hasResourceAccess(accessShortName, statusShortName, vi.imageInstanceUri);
        if (al == AccessLevel.MIXED || al == AccessLevel.NOACCESS) {
            return ResponseEntity.status(HttpStatus.resolve(acc.isUserLoggedIn() ? 403 : 401)).cacheControl(CacheControl.noCache())
                    .body(getStream("Insufficient rights"));
        }
        if (vi.iiifManifestUri != null) {
            logger.info("redirect manifest request for ID {} to {}", identifier, vi.iiifManifestUri.toString());
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.add("Location", vi.iiifManifestUri.toString());
            return new ResponseEntity<StreamingResponseBody>(responseHeaders, HttpStatus.resolve(302));
        }
        if (wo == null && (id.getSubType() == Identifier.MANIFEST_ID_VOLUMEID_OUTLINE
                || id.getSubType() == Identifier.MANIFEST_ID_WORK_IN_VOLUMEID_OUTLINE)) {
            try {
                // important: we take the outline of the whole root work, that makes
                // caching more efficient
                wo = InstanceOutlineService.Instance.getAsync(vi.instanceUri).get();
                String shortWorkId = id.getInstanceId();
                if (shortWorkId == null)
                    shortWorkId = "bdr:" + vi.instanceUri.substring(AppConstants.BDR_len);
                rootPart = wo.getPartForInstanceId(shortWorkId);
                // TODO: case of a virtual work pointing to an outline
                // we should copy the labels of the virtual work...
            } catch (InterruptedException | ExecutionException e) {
                throw new BDRCAPIException(500, AppConstants.GENERIC_IDENTIFIER_ERROR, e);
            }
        }
        if (rootPart == null) {
            // case of a virtual work with a location
            if (iInf == null && id.getInstanceId() != null) {
                try {
                    iInf = InstanceInfoService.Instance.getAsync(id.getInstanceId()).get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new BDRCAPIException(500, AppConstants.GENERIC_IDENTIFIER_ERROR, e);
                }
            }
            rootPart = iInf;
        }
        // TODO: case where a part is asked with an outline, we need to make sure that
        // we get the part asked by the user
        final Manifest resmanifest = ManifestService.getManifestForIdentifier(isAdmin, id, vi, continuous, volumeId, al == AccessLevel.FAIR_USE,
                rootPart);
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
            HttpServletRequest req, HttpServletResponse resp) throws BDRCAPIException {
        return getCanvas(identifier, "", filename, req, resp);
    }

    @RequestMapping(value = "/{version:.+}/{identifier}/canvas/{filename}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<StreamingResponseBody> getCanvas(@PathVariable String identifier, @PathVariable String version,
            @PathVariable String filename, HttpServletRequest req, HttpServletResponse resp) throws BDRCAPIException {
        resp.setContentType("application/json;charset=UTF-8");
        Identifier id = null;
        try {
            id = new Identifier(identifier, Identifier.MANIFEST_ID);
        } catch (IdentifierException e) {
            throw new BDRCAPIException(404, AppConstants.GENERIC_IDENTIFIER_ERROR, e);
        }
        ImageGroupInfo vi;
        try {
            vi = ImageGroupInfoService.Instance.getAsync(id.getImageGroupId()).get();
        } catch (InterruptedException | ExecutionException e1) {
            throw new BDRCAPIException(500, AppConstants.GENERIC_IDENTIFIER_ERROR, e1);
        } // not entirely sure about the false
        if (vi.restrictedInChina && GeoLocation.isFromChina(req)) {
            return ResponseEntity.status(HttpStatus.resolve(403)).cacheControl(CacheControl.noCache()).body(getStream("Insufficient rights"));
        }
        Access acc = (Access) req.getAttribute("access");
        if (acc == null)
            acc = new Access();
        final String accessShortName = getLocalName(vi.access.getUri());
        final String statusShortName = getLocalName(vi.statusUri);
        final AccessLevel al = acc.hasResourceAccess(accessShortName, statusShortName, vi.imageInstanceUri);
        if (al == AccessLevel.MIXED || al == AccessLevel.NOACCESS) {
            return ResponseEntity.status(HttpStatus.resolve(acc.isUserLoggedIn() ? 403 : 401)).cacheControl(CacheControl.noCache())
                    .body(getStream("Insufficient rights"));
        }
        if (vi.iiifManifestUri != null) {
            return ResponseEntity.status(HttpStatus.resolve(404)).cacheControl(CacheControl.noCache())
                    .body(getStream("\"Cannot serve canvas for external manifests\""));
        }
        ImageInfoList imageInfoList;
        try {
            imageInfoList = ImageInfoListService.Instance.getAsync(vi.imageInstanceUri.substring(AppConstants.BDR_len), vi.imageGroup).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new BDRCAPIException(500, AppConstants.GENERIC_IDENTIFIER_ERROR, e);
        }
        final Integer imgSeqNum = imageInfoList.getSeqNumFromFilename(filename);
        if (imgSeqNum == null)
            throw new BDRCAPIException(500, AppConstants.GENERIC_LDS_ERROR, "Cannot find filename in the S3 image list");
        if (al == AccessLevel.FAIR_USE) {
            if (imgSeqNum > (AppConstants.FAIRUSE_PAGES_S + vi.pagesIntroTbrc) && imgSeqNum < (imageInfoList.size() - AppConstants.FAIRUSE_PAGES_E)) {
                return ResponseEntity.status(HttpStatus.resolve(acc.isUserLoggedIn() ? 403 : 401)).cacheControl(CacheControl.noCache())
                        .body(getStream("\"Insufficient rights (" + vi.access + ")\""));
            }
        }
        final Canvas res = ManifestService.getCanvasForIdentifier(id, vi, imgSeqNum, id.getImageGroupId(), imageInfoList);
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
        ImageGroupInfo vi;
        try {
            vi = ImageGroupInfoService.Instance.getAsync(volumeId).get();
        } catch (InterruptedException | ExecutionException e1) {
            throw new BDRCAPIException(404, AppConstants.GENERIC_IDENTIFIER_ERROR, e1);
        }
        ImageInfoList imageInfoList;
        try {
            imageInfoList = ImageInfoListService.Instance.getAsync(vi.imageInstanceUri.substring(AppConstants.BDR_len), vi.imageGroup).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new BDRCAPIException(404, AppConstants.GENERIC_IDENTIFIER_ERROR, e);
        }
        return ResponseEntity.status(HttpStatus.OK)
                .cacheControl(CacheControl.maxAge(Long.parseLong(AuthProps.getProperty("max-age")), TimeUnit.SECONDS).cachePublic())
                .body(imageInfoList.list);
    }

    public static String getLocalName(final String st) {
        if (st == null || st.isEmpty()) {
            return null;
        }
        return st.substring(st.lastIndexOf("/") + 1);
    }

    @RequestMapping(value = "/bvm/ig:{resourceQname}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<StreamingResponseBody> getImageInfoFile(@PathVariable String resourceQname, HttpServletRequest request,
            HttpServletResponse resp) throws BDRCAPIException {
        if (!resourceQname.startsWith("bdr:I"))
            throw new BDRCAPIException(404, AppConstants.GENERIC_APP_ERROR_CODE, "no resource " + resourceQname);
        BVM bvm;
        try {
            bvm = BVMService.Instance.getAsync(resourceQname.substring(4)).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new BDRCAPIException(404, AppConstants.GENERIC_IDENTIFIER_ERROR, e);
        }
        if (bvm == null) {
            throw new BDRCAPIException(404, AppConstants.GENERIC_IDENTIFIER_ERROR, "resource not available");
        }
        return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON_UTF8)
                .cacheControl(CacheControl.maxAge(Long.parseLong(AuthProps.getProperty("max-age")), TimeUnit.SECONDS).cachePublic())
                .body(getStream(bvm, BVMService.om));
    }

    public static String getTwoLettersBucket(String st) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            // this is too stupid to throw
            return "";
        }
        md.reset();
        md.update(st.getBytes(Charset.forName("UTF8")));
        return new String(Hex.encodeHex(md.digest())).substring(0, 2);
    }

    @RequestMapping(value = "/bvm/ig:{resourceQname}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<String> writeImageInfoFile(@PathVariable String resourceQname, @RequestBody String json, HttpServletRequest request,
            HttpServletResponse resp) throws BDRCAPIException {
        BVM bvm;
        if (!resourceQname.startsWith("bdr:I"))
            throw new BDRCAPIException(404, AppConstants.GENERIC_APP_ERROR_CODE, "no resource " + resourceQname);
        try {
            bvm = BVMService.om.readValue(json, BVM.class);
        } catch (IOException e) {
            throw new BDRCAPIException(400, AppConstants.GENERIC_APP_ERROR_CODE, e.getMessage());
        }
        bvm.validate();
        ChangeLogItem cli = bvm.getLatestChangeLogItem();
        // TODO: check last change against current user
        if (!bvm.imageGroupQname.equals(resourceQname))
            throw new BDRCAPIException(422, GENERIC_APP_ERROR_CODE, "invalid bvm: for-volume doesn't match the resource url " + resourceQname);
        String repoBase = System.getProperty("user.dir") + "/gitData/buda-volume-manifests/";
        Repository repo = GitHelpers.ensureGitRepo(repoBase);
        BVMService.pullIfNecessary();
        final String resourceLocalName = resourceQname.substring(4);
        final String twoLetters = getTwoLettersBucket(resourceLocalName);
        final String filename = repoBase + twoLetters + "/" + resourceLocalName + ".json";
        final File dir = new File(repoBase + twoLetters + "/");
        if (!dir.exists()) {
            dir.mkdir();
        }
        // super basic multiversion concurrency control, à la CouchDB
        final File f = new File(filename);
        boolean created = false;
        if (f.exists()) {
            BVM oldBvm = null;
            try {
                oldBvm = BVMService.Instance.getAsync(resourceQname.substring(4)).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new BDRCAPIException(404, AppConstants.GENERIC_IDENTIFIER_ERROR, e);
            }
            if (oldBvm != null) {
                if (oldBvm.rev != null && !oldBvm.rev.equals(bvm.rev)) {
                    // temporarily disable to ease tests
                    // throw new BDRCAPIException(409, AppConstants.GENERIC_APP_ERROR_CODE,
                    // "document update conflict, please update to the latest version.");
                } else if (oldBvm.rev == null) {
                    throw new BDRCAPIException(500, AppConstants.GENERIC_APP_ERROR_CODE, "old bvm doesn't have a rev");
                }
            } else {
                created = true;
            }
        } else {
            created = true;
        }
        String newrev = UUID.randomUUID().toString();
        bvm.rev = newrev;
        try {
            BVMService.om.writer(BVMService.printer).writeValue(f, bvm);
        } catch (IOException e) {
            throw new BDRCAPIException(500, AppConstants.GENERIC_APP_ERROR_CODE, "error when writing bvm");
        }
        GitHelpers.commitChanges(repo, cli.message.value);
        BVMService.pushWhenNecessary();
        BVMService.Instance.putInCache(bvm, resourceLocalName);
        return ResponseEntity.status(created ? HttpStatus.CREATED : HttpStatus.OK).eTag(newrev)
                // TODO: add location? sort of expected for HttpStatus 201
                .contentType(MediaType.APPLICATION_JSON_UTF8).body("{\"ok:\" true, \"rev\": \"" + newrev + "\"}");
    }

    @PostMapping(value = "/callbacks/github/bdrc-auth")
    public ResponseEntity<String> updateAuthModel() {
        logger.info("updating Auth data model() >>");
        RdfAuthModel.readAuthModel();
        return ResponseEntity.ok("Updated auth Model was read");
    }

    public static String getShortName(final String st) {
        if (st == null || st.isEmpty()) {
            return null;
        }
        return st.substring(st.lastIndexOf("/") + 1);
    }

    private StreamingResponseBody getStream(Object obj, ObjectMapper om) {
        final StreamingResponseBody stream = new StreamingResponseBody() {
            @Override
            public void writeTo(final OutputStream os) throws IOException {
                om.writer().writeValue(os, obj);
            }
        };
        return stream;
    }

    private StreamingResponseBody getStream(Object obj) {
        return getStream(obj, AppConstants.IIIFMAPPER);
    }
}
