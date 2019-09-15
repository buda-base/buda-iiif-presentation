package io.bdrc.iiif.presentation;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.digitalcollections.iiif.model.sharedcanvas.Canvas;
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
import io.bdrc.libraries.Identifier;
import io.bdrc.libraries.IdentifierException;

@Component
@RestController
@RequestMapping("/")
public class IIIFPresentationService {

	private static final Logger logger = LoggerFactory.getLogger(IIIFPresentationService.class);
	static final int ACCESS_CONTROL_MAX_AGE_IN_SECONDS = 24 * 60 * 60;

	final static ObjectMapper om = new ObjectMapper();

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
	@RequestMapping(value = "/admin/cache/{region}/{key}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
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
		// return new ResponseEntity<StreamingResponseBody>(stream, HttpStatus.OK);
		case AppConstants.CACHEPREFIX_WO:
			return ResponseEntity.ok().body((WorkOutline) res);
		// return new ResponseEntity<StreamingResponseBody>(stream, HttpStatus.OK);
		case AppConstants.CACHEPREFIX_II:
			return ResponseEntity.ok().body((ItemInfo) res);
		// return new ResponseEntity<StreamingResponseBody>(stream, HttpStatus.OK);
		case AppConstants.CACHEPREFIX_IIL:
			return ResponseEntity.ok().body((List<ImageInfo>) res);
		// return new ResponseEntity<StreamingResponseBody>(stream, HttpStatus.OK);
		case AppConstants.CACHEPREFIX_VI:
			return ResponseEntity.ok().body((VolumeInfo) res);
		// return new ResponseEntity<StreamingResponseBody>(stream, HttpStatus.OK);
		default:
			throw new BDRCAPIException(500, AppConstants.GENERIC_IDENTIFIER_ERROR, "unhandled key prefix, this shouldn't happen");
		}
	}

	@RequestMapping(value = "/collection/{identifier}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> getCollectionNoVer(@PathVariable String identifier, HttpServletRequest request) throws BDRCAPIException {
		return getCollection(identifier, "", request);
	}

	@RequestMapping(value = "/{version:.+}/collection/{identifier}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> getCollection(@PathVariable String identifier, @PathVariable String version, HttpServletRequest request) throws BDRCAPIException {
		System.out.println("Call to getCollection()");
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
				throw new BDRCAPIException(404, AppConstants.GENERIC_IDENTIFIER_ERROR, e2);
			}
			access = winf1.rootAccess;
			statusUri = winf1.rootStatus;
			restrictedInChina = winf1.rootRestrictedInChina;
			itemId = winf1.itemId;
			break;
		}
		if (restrictedInChina && GeoLocation.isFromChina(request)) {
			throw new BDRCAPIException(403, AppConstants.GENERIC_LDS_ERROR, "Insufficient rights");
		}
		Access acc = (Access) request.getAttribute("access");
		if (acc == null)
			acc = new Access();
		final String accessShortName = getShortName(access.getUri());
		final String statusShortName = getShortName(statusUri);
		final AccessLevel al = acc.hasResourceAccess(accessShortName, statusShortName, itemId);
		if (al == AccessLevel.MIXED || al == AccessLevel.NOACCESS) {
			return ResponseEntity.status(acc.isUserLoggedIn() ? 403 : 401).cacheControl(CacheControl.noCache()).body("\"Insufficient rights\"");
		}
		int maxAgeSeconds = Integer.parseInt(AuthProps.getProperty("max-age")) / 1000;
		if (access == AccessType.OPEN) {
			return ResponseEntity.ok().cacheControl(CacheControl.maxAge(maxAgeSeconds, TimeUnit.SECONDS).cachePublic()).body(CollectionService.getCollectionForIdentifier(id, continuous));
		} else {
			return ResponseEntity.ok().cacheControl(CacheControl.maxAge(maxAgeSeconds, TimeUnit.SECONDS).cachePrivate()).body(CollectionService.getCollectionForIdentifier(id, continuous));
		}
	}

	@RequestMapping(value = "/{identifier}/manifest", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> getManifestNoVer(@PathVariable String identifier, HttpServletRequest req) throws BDRCAPIException {
		return getManifest(identifier, "", req);
	}

	@RequestMapping(value = "/{version:.+}/{identifier}/manifest", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> getManifest(@PathVariable String identifier, @PathVariable String version, HttpServletRequest req) throws BDRCAPIException {
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
		if (id.getWorkId() != null && (id.getSubType() == Identifier.MANIFEST_ID_WORK_IN_ITEM || id.getSubType() == Identifier.MANIFEST_ID_WORK_IN_VOLUMEID)) {
			try {
				wi = WorkInfoService.Instance.getAsync(id.getWorkId()).get();
				rootPart = wi;
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
			if (volumeId == null)
				return ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(CacheControl.noCache()).body("\"Cannot find volume ID\"");
		}
		VolumeInfo vi;
		try {
			vi = VolumeInfoService.Instance.getAsync(volumeId).get();
		} catch (InterruptedException | ExecutionException e) {
			throw new BDRCAPIException(500, AppConstants.GENERIC_IDENTIFIER_ERROR, e);
		}
		if (vi.restrictedInChina && GeoLocation.isFromChina(req)) {
			return ResponseEntity.status(HttpStatus.resolve(403)).cacheControl(CacheControl.noCache()).body("Insufficient rights");
		}
		Access acc = (Access) req.getAttribute("access");
		if (acc == null)
			acc = new Access();
		final String accessShortName = getShortName(vi.access.getUri());
		final String statusShortName = getShortName(vi.statusUri);
		final AccessLevel al = acc.hasResourceAccess(accessShortName, statusShortName, vi.itemId);
		if (al == AccessLevel.MIXED || al == AccessLevel.NOACCESS) {
			return ResponseEntity.status(HttpStatus.resolve(acc.isUserLoggedIn() ? 403 : 401)).cacheControl(CacheControl.noCache()).body("Insufficient rights");
		}
		if (vi.iiifManifest != null) {
			logger.info("redirect manifest request for ID {} to {}", identifier, vi.iiifManifest.toString());
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.add("Location", vi.iiifManifest.toString());
			return new ResponseEntity<Object>(responseHeaders, HttpStatus.resolve(302));
		}
		if (wo == null && (id.getSubType() == Identifier.MANIFEST_ID_VOLUMEID_OUTLINE || id.getSubType() == Identifier.MANIFEST_ID_WORK_IN_VOLUMEID_OUTLINE)) {
			try {
				// important: we take the outline of the whole root work, that makes
				// caching more efficient
				wo = WorkOutlineService.Instance.getAsync(vi.workId).get();
				String shortWorkId = id.getWorkId();
				if (shortWorkId == null)
					shortWorkId = "bdr:" + vi.workId.substring(AppConstants.BDR_len);
				rootPart = wo.getPartForWorkId(shortWorkId);
			} catch (InterruptedException | ExecutionException e) {
				throw new BDRCAPIException(500, AppConstants.GENERIC_IDENTIFIER_ERROR, e);
			}
		}
		// TODO: case where a part is asked with an outline, we need to make sure that
		// we get the part asked by the user
		final Manifest resmanifest = ManifestService.getManifestForIdentifier(id, vi, continuous, volumeId, al == AccessLevel.FAIR_USE, rootPart);
		if (vi.access == AccessType.OPEN) {
			return ResponseEntity.status(HttpStatus.OK).cacheControl(CacheControl.maxAge(Long.parseLong(AuthProps.getProperty("max-age")), TimeUnit.SECONDS).cachePublic()).body(resmanifest);
		} else {
			return ResponseEntity.status(HttpStatus.OK).cacheControl(CacheControl.maxAge(Long.parseLong(AuthProps.getProperty("max-age")), TimeUnit.SECONDS).cachePrivate()).body(resmanifest);
		}
	}

	@RequestMapping(value = "/{identifier}/canvas/{filename}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> getCanvasNoVer(@PathVariable String identifier, @PathVariable String version, @PathVariable String filename, HttpServletRequest req) throws BDRCAPIException {
		return getCanvas(identifier, "", filename, req);
	}

	@RequestMapping(value = "/{version:.+}/{identifier}/canvas/{filename}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> getCanvas(@PathVariable String identifier, @PathVariable String version, @PathVariable String filename, HttpServletRequest req) throws BDRCAPIException {
		// TODO: adjust to new filename in the path (requires file name lookup in the
		// image list)
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
			return ResponseEntity.status(HttpStatus.resolve(403)).cacheControl(CacheControl.noCache()).body("Insufficient rights");
		}
		Access acc = (Access) req.getAttribute("access");
		if (acc == null)
			acc = new Access();
		final String accessShortName = getShortName(vi.access.getUri());
		final String statusShortName = getShortName(vi.statusUri);
		final AccessLevel al = acc.hasResourceAccess(accessShortName, statusShortName, vi.itemId);
		if (al == AccessLevel.MIXED || al == AccessLevel.NOACCESS) {
			return ResponseEntity.status(HttpStatus.resolve(acc.isUserLoggedIn() ? 403 : 401)).cacheControl(CacheControl.noCache()).body("Insufficient rights");
		}
		if (vi.iiifManifest != null) {
			return ResponseEntity.status(HttpStatus.resolve(404)).cacheControl(CacheControl.noCache()).body("\"Cannot serve canvas for external manifests\"");
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
			if (imgSeqNum > (AppConstants.FAIRUSE_PAGES_S + vi.pagesIntroTbrc) && imgSeqNum < (vi.totalPages - AppConstants.FAIRUSE_PAGES_E))
				return ResponseEntity.status(HttpStatus.resolve(acc.isUserLoggedIn() ? 403 : 401)).cacheControl(CacheControl.noCache()).body("\"Insufficient rights (" + vi.access + ")\"");
		}
		final Canvas res = ManifestService.getCanvasForIdentifier(id, vi, imgSeqNum, id.getVolumeId(), imageInfoList);
		if (vi.access == AccessType.OPEN) {
			return ResponseEntity.status(HttpStatus.OK).cacheControl(CacheControl.maxAge(Long.parseLong(AuthProps.getProperty("max-age")), TimeUnit.SECONDS).cachePublic()).body(res);
		} else {
			return ResponseEntity.status(HttpStatus.OK).cacheControl(CacheControl.maxAge(Long.parseLong(AuthProps.getProperty("max-age")), TimeUnit.SECONDS).cachePrivate()).body(res);
		}
	}

	public static String getShortName(final String st) {
		if (st == null || st.isEmpty()) {
			return null;
		}
		return st.substring(st.lastIndexOf("/") + 1);
	}
}