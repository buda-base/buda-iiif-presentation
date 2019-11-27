package io.bdrc.iiif.presentation;

import static io.bdrc.iiif.presentation.AppConstants.BDR;
import static io.bdrc.iiif.presentation.AppConstants.BDR_len;
import static io.bdrc.iiif.presentation.AppConstants.GENERIC_APP_ERROR_CODE;
import static io.bdrc.iiif.presentation.AppConstants.IIIFPresPrefix;
import static io.bdrc.iiif.presentation.AppConstants.IIIF_IMAGE_PREFIX;
import static io.bdrc.iiif.presentation.AppConstants.NO_ACCESS_ERROR_CODE;
import static io.bdrc.iiif.presentation.AppConstants.PDF_URL_PREFIX;
import static io.bdrc.iiif.presentation.AppConstants.ZIP_URL_PREFIX;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.digitalcollections.iiif.model.ImageContent;
import de.digitalcollections.iiif.model.OtherContent;
import de.digitalcollections.iiif.model.PropertyValue;
import de.digitalcollections.iiif.model.enums.ViewingDirection;
import de.digitalcollections.iiif.model.image.ImageApiProfile;
import de.digitalcollections.iiif.model.sharedcanvas.Canvas;
import de.digitalcollections.iiif.model.sharedcanvas.Manifest;
import de.digitalcollections.iiif.model.sharedcanvas.Range;
import de.digitalcollections.iiif.model.sharedcanvas.Sequence;
import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.resmodels.BDRCPresentationImageService;
import io.bdrc.iiif.presentation.resmodels.ImageInfo;
import io.bdrc.iiif.presentation.resmodels.LangString;
import io.bdrc.iiif.presentation.resmodels.Location;
import io.bdrc.iiif.presentation.resmodels.PartInfo;
import io.bdrc.iiif.presentation.resmodels.VolumeInfo;
import io.bdrc.iiif.presentation.resmodels.WorkOutline;
import io.bdrc.iiif.presentation.resservices.ImageInfoListService;
import io.bdrc.libraries.Identifier;

public class ManifestService {

    private static final Logger logger = LoggerFactory.getLogger(ManifestService.class);

    public static final Map<String, Locale> locales = new HashMap<>();
    public static final PropertyValue attribution = new PropertyValue();
    // public static final List<ViewingHint> VIEW_HINTS=Arrays.asList(new
    // ViewingHint[] { ViewingHint.CONTINUOUS});
    public static final String VIEWING_HINTS = "continuous";

    static {
        attribution.addValue(getLocaleFor("en"), "Buddhist Digital Resource Center");
        attribution.addValue(getLocaleFor("bo"), "ནང་བསྟན་དཔེ་ཚོགས་ལྟེ་གནས།");
        attribution.addValue(getLocaleFor("zh"), "佛教数字资源中心（BDRC）");
    }

    public static Locale getLocaleFor(String lt) {
        return locales.computeIfAbsent(lt, x -> Locale.forLanguageTag(lt));
    }

    public static PropertyValue getPropForLabels(List<LangString> labels) {
        if (labels == null)
            return null;
        final PropertyValue res = new PropertyValue();
        for (final LangString ls : labels) {
            // TODO: does it work well or should it be grouped by language first?
            if (ls.language != null) {
                res.addValue(getLocaleFor(ls.language), ls.value);
            } else {
                res.addValue(ls.value);
            }
        }
        return res;
    }

    public static PropertyValue getLabelForImage(final int imageIndex, final VolumeInfo vi) {
        final PropertyValue res = new PropertyValue();
        if (imageIndex < (vi.pagesIntroTbrc + 1)) {
            // this shouldn't really happen anymore
            res.addValue(getLocaleFor("en"), "Bdrc-" + imageIndex);
            return res;
        }
        res.addValue(getLocaleFor("en"), "p. " + imageIndex);
        res.addValue(getLocaleFor("bo-x-ewts"), Integer.toString(imageIndex));
        return res;
    }

    public static String getImageServiceUrl(final String filename, final String volumeId) {
        return IIIF_IMAGE_PREFIX + volumeId + "::" + filename;
    }

    // TODO: not all calls to this function profile the filename argument, but they
    // should and the canvas should
    // have the image filename, not the image seqnum
    public static String getCanvasUri(final String filename, final String volumeId, final int seqNum) {
        // seqNum starts at 1
        return IIIFPresPrefix + "v:" + volumeId + "/canvas/" + filename;
        // return IIIFPresPrefix+"v:"+volumeId+"/canvas"+"/"+seqNum;
    }

    public static ViewingDirection getViewingDirection(final List<ImageInfo> imageInfoList) {
        if (imageInfoList.size() < 3) {
            return ViewingDirection.LEFT_TO_RIGHT;
        }
        final ImageInfo p3Info = imageInfoList.get(2);
        if (p3Info.height > p3Info.width) {
            return ViewingDirection.LEFT_TO_RIGHT;
        } else {
            return ViewingDirection.TOP_TO_BOTTOM;
        }
    }

    public static Canvas addOneCanvas(final int imgSeqNum, final Identifier id, final List<ImageInfo> imageInfoList, final VolumeInfo vi, final String volumeId, final Sequence mainSeq) {
        final ImageInfo imageInfo = imageInfoList.get(imgSeqNum - 1);
        final Integer size = imageInfo.size;
        if (size != null && size > 1000000)
            return null;
        final Canvas canvas = buildCanvas(id, imgSeqNum, imageInfoList, volumeId, vi);
        mainSeq.addCanvas(canvas);
        return canvas;
    }

    public static Canvas addCopyrightCanvas(final Sequence mainSeq, final String volumeId) {
        final String canvasUri = IIIFPresPrefix + "v:" + volumeId + "/canvas" + "/" + AppConstants.COPYRIGHT_PAGE_CANVAS_ID;
        final Canvas canvas = new Canvas(canvasUri);
        canvas.setWidth(AppConstants.COPYRIGHT_PAGE_W);
        canvas.setHeight(AppConstants.COPYRIGHT_PAGE_H);
        final String imageServiceUrl = IIIF_IMAGE_PREFIX + AppConstants.COPYRIGHT_PAGE_IMG_ID;
        ImageApiProfile profile = ImageApiProfile.LEVEL_ZERO;
        final BDRCPresentationImageService imgServ = new BDRCPresentationImageService(imageServiceUrl, profile);
        final String imgUrl;
        if (AppConstants.COPYRIGHT_PAGE_IS_PNG) {
            imgUrl = imageServiceUrl + "/full/max/0/default.png";
            imgServ.setPreferredFormats(pngHint);
        } else {
            imgUrl = imageServiceUrl + "/full/max/0/default.jpg";
        }
        imgServ.setHeight(AppConstants.COPYRIGHT_PAGE_W);
        imgServ.setWidth(AppConstants.COPYRIGHT_PAGE_W);
        final ImageContent img = new ImageContent(imgUrl);
        img.addService(imgServ);
        img.setWidth(AppConstants.COPYRIGHT_PAGE_W);
        img.setHeight(AppConstants.COPYRIGHT_PAGE_W);
        canvas.addImage(img);
        mainSeq.addCanvas(canvas);
        return canvas;
    }

    public static Sequence getSequenceFrom(final Identifier id, final List<ImageInfo> imageInfoList, final VolumeInfo vi, final String volumeId, final int beginIndex, final int endIndex, final boolean fairUse) throws BDRCAPIException {
        final Sequence mainSeq = new Sequence(IIIFPresPrefix + id.getId() + "/sequence/main");
        // all indices start at 1
        mainSeq.setViewingDirection(getViewingDirection(imageInfoList));
        Canvas firstCanvas = null;
        final int totalPages = imageInfoList.size();
        //if (totalPages != vi.totalPages)
        //    logger.error("VolumeInfo has a different image number than the json file ("+vi.totalPages+" vs. "+totalPages+") for identifier "+id.getId());
        if (!fairUse) {
            for (int imgSeqNum = beginIndex; imgSeqNum <= endIndex; imgSeqNum++) {
                final Canvas thisCanvas = addOneCanvas(imgSeqNum, id, imageInfoList, vi, volumeId, mainSeq);
                if (firstCanvas == null)
                    firstCanvas = thisCanvas;
            }
        } else {
            final int firstUnaccessiblePage = AppConstants.FAIRUSE_PAGES_S + vi.pagesIntroTbrc + 1;
            final int lastUnaccessiblePage = totalPages - AppConstants.FAIRUSE_PAGES_E;
            // first part: min(firstUnaccessiblePage+1,beginIndex) to
            // min(endIndex,firstUnaccessiblePage+1)
            for (int imgSeqNum = Math.min(firstUnaccessiblePage, beginIndex); imgSeqNum <= Math.min(endIndex, firstUnaccessiblePage - 1); imgSeqNum++) {
                final Canvas thisCanvas = addOneCanvas(imgSeqNum, id, imageInfoList, vi, volumeId, mainSeq);
                if (firstCanvas == null)
                    firstCanvas = thisCanvas;
            }
            // then copyright page, if either beginIndex or endIndex is
            // > FAIRUSE_PAGES_S+tbrcintro and < vi.totalPages-FAIRUSE_PAGES_E
            if ((beginIndex >= firstUnaccessiblePage && beginIndex <= lastUnaccessiblePage) || (endIndex >= firstUnaccessiblePage && endIndex <= lastUnaccessiblePage) || (beginIndex < firstUnaccessiblePage && endIndex > lastUnaccessiblePage)) {
                final Canvas thisCanvas = addCopyrightCanvas(mainSeq, volumeId);
                if (firstCanvas == null)
                    firstCanvas = thisCanvas;
            }
            // last part: max(beginIndex,lastUnaccessiblePage) to
            // max(endIndex,lastUnaccessiblePage)
            for (int imgSeqNum = Math.max(lastUnaccessiblePage + 1, beginIndex); imgSeqNum <= Math.max(endIndex, lastUnaccessiblePage); imgSeqNum++) {
                final Canvas thisCanvas = addOneCanvas(imgSeqNum, id, imageInfoList, vi, volumeId, mainSeq);
                if (firstCanvas == null)
                    firstCanvas = thisCanvas;
            }
        }
        mainSeq.setStartCanvas(firstCanvas.getIdentifier());
        return mainSeq;
    }

    public static PropertyValue getLabel(final int volumeNumber, final List<LangString> labels, final boolean needsVolumeIndication) {
        final PropertyValue label = new PropertyValue();
        final String volumeNum = Integer.toString(volumeNumber);
        if (labels == null || labels.isEmpty()) {
            label.addValue(ManifestService.getLocaleFor("en"), "volume " + volumeNum);
            label.addValue(ManifestService.getLocaleFor("bo-x-ewts"), "pod/_" + volumeNum);
            return label;
        }
        for (LangString ls : labels) {
            if (ls.language != null) {
                if (ls.language.equals("bo-x-ewts") && needsVolumeIndication)
                    label.addValue(ManifestService.getLocaleFor(ls.language), ls.value + "_(pod/_" + volumeNum + ")");
                else
                    label.addValue(ManifestService.getLocaleFor(ls.language), ls.value);
            } else {
                label.addValue(ls.value);
            }
        }
        return label;
    }

    public static Manifest getManifestForIdentifier(final Identifier id, final VolumeInfo vi, boolean continuous, final String volumeId, final boolean fairUse, final PartInfo rootPart) throws BDRCAPIException {
        if (id.getType() != Identifier.MANIFEST_ID || (id.getSubType() != Identifier.MANIFEST_ID_VOLUMEID && id.getSubType() != Identifier.MANIFEST_ID_WORK_IN_VOLUMEID && id.getSubType() != Identifier.MANIFEST_ID_VOLUMEID_OUTLINE && id.getSubType() != Identifier.MANIFEST_ID_WORK_IN_VOLUMEID_OUTLINE)) {
            throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "you cannot access this type of manifest yet");
        }
        if (!vi.workId.startsWith(BDR)) {
            throw new BDRCAPIException(403, NO_ACCESS_ERROR_CODE, "you can only access BDRC volumes through this API");
        }
        logger.info("building manifest for ID {}", id.getId());
        logger.debug("rootPart: {}", rootPart);
        final String workLocalId = vi.workId.substring(BDR_len);
        List<ImageInfo> imageInfoList;
        try {
            imageInfoList = ImageInfoListService.Instance.getAsync(workLocalId, vi.imageGroup).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, e);
        }
        final Manifest manifest = new Manifest(IIIFPresPrefix + id.getId() + "/manifest");
        manifest.setAttribution(attribution);
        manifest.addLicense("https://creativecommons.org/publicdomain/mark/1.0/");
        manifest.addLogo(IIIF_IMAGE_PREFIX + "static::logo.png/full/max/0/default.png");
        manifest.setLabel(getLabel(vi.volumeNumber, (rootPart == null ? null : rootPart.labels), true)); // TODO: the final true shouldn't always be true
        int nbPagesIntro = vi.pagesIntroTbrc;
        int bPage;
        int ePage;
        final int totalPages = imageInfoList.size();
        if (id.getSubType() == Identifier.MANIFEST_ID_WORK_IN_VOLUMEID || id.getSubType() == Identifier.MANIFEST_ID_WORK_IN_VOLUMEID_OUTLINE) {
            bPage = 1 + nbPagesIntro;
            ePage = totalPages;
            Location location = (rootPart == null) ? null : rootPart.location;
            if (location != null) {
                if (location.bvolnum > vi.volumeNumber)
                    throw new BDRCAPIException(404, NO_ACCESS_ERROR_CODE, "the work you asked starts after this volume");
                // if bvolnum < vi.volumeNumber, we already have bPage correctly set to:
                // 1+nbPagesIntro
                if (location.bvolnum.equals(vi.volumeNumber))
                    bPage = location.bpagenum;
                if (location.evolnum < vi.volumeNumber)
                    throw new BDRCAPIException(404, NO_ACCESS_ERROR_CODE, "the work you asked ends before this volume");
                // if evolnum > vi.volumeNumber, we already have bPage correctly set to totalPages
                if (vi.volumeNumber.equals(location.evolnum)  && location.epagenum != -1)
                    ePage = location.epagenum;
            }
        } else {
            bPage = id.getBPageNum() == null ? 1 + nbPagesIntro : id.getBPageNum().intValue();
            ePage = id.getEPageNum() == null ? totalPages : id.getEPageNum().intValue();
        }
        logger.debug("computed: {}-{}", bPage, ePage);
        final Sequence mainSeq = getSequenceFrom(id, imageInfoList, vi, volumeId, bPage, ePage, fairUse);
        mainSeq.setViewingDirection(ViewingDirection.TOP_TO_BOTTOM);
        if (continuous) {
            mainSeq.setViewingHints(VIEWING_HINTS);
        }
        // PDF / zip download
        final List<OtherContent> oc = getRenderings(volumeId, bPage, ePage);
        manifest.setRenderings(oc);
        if (id.getSubType() == Identifier.MANIFEST_ID_VOLUMEID_OUTLINE || id.getSubType() == Identifier.MANIFEST_ID_WORK_IN_VOLUMEID_OUTLINE) {
            addRangesToManifest(manifest, id, vi, volumeId, fairUse, imageInfoList, rootPart);
        }
        manifest.addSequence(mainSeq);
        return manifest;
    }

    public static List<OtherContent> getRenderings(final String volumeId, final int bPage, final int ePage) {
        final String fullId = volumeId + "::" + bPage + "-" + ePage;
        final OtherContent oct = new OtherContent(PDF_URL_PREFIX + "v:" + fullId, "application/pdf");
        oct.setLabel(new PropertyValue("Download as PDF"));
        final OtherContent oct1 = new OtherContent(ZIP_URL_PREFIX + "v:" + fullId, "application/zip");
        oct1.setLabel(new PropertyValue("Download as ZIP"));
        final ArrayList<OtherContent> ct = new ArrayList<>();
        ct.add(oct);
        ct.add(oct1);
        return ct;
    }

    public static void addRangesToManifest(final Manifest m, final Identifier id, final VolumeInfo vi, final String volumeId, final boolean fairUse, final List<ImageInfo> imageInfoList, final PartInfo rootPi) throws BDRCAPIException {
        if (rootPi == null)
            return;
        final PartInfo volumeRoot = WorkOutline.getRootPiForVolumeR(rootPi, vi.volumeNumber);
        if (volumeRoot == null)
            return;
        final Range r = new Range(IIIFPresPrefix + "v:" + id.getVolumeId() + "/range/top", "Table of Contents");
        r.setViewingHints("top");
        for (final PartInfo part : volumeRoot.parts) {
            addSubRangeToRange(m, r, id, part, vi, volumeId, imageInfoList, fairUse);
        }
        m.addRange(r);
    }

    public static void addSubRangeToRange(final Manifest m, final Range r, final Identifier id, final PartInfo part, final VolumeInfo vi, final String volumeId, final List<ImageInfo> imageInfoList, final boolean fairUse) throws BDRCAPIException {
        // do not add ranges where there is no location nor subparts
        if (part.location == null && part.parts == null)
            return;
        final String rangeUri = IIIFPresPrefix + "v:" + volumeId + "/range/w:" + part.partId;
        final Range subRange = new Range(rangeUri);
        final PropertyValue labels = getPropForLabels(part.labels);
        subRange.setLabel(labels);
        if (part.location != null) {
            final Location loc = part.location;
            if (loc.bvolnum > vi.volumeNumber || loc.evolnum < vi.volumeNumber)
                return;
            int bPage = 1;
            if (loc.bvolnum.equals(vi.volumeNumber) && loc.bpagenum != null)
                bPage = loc.bpagenum;
            // ignoring the tbrc pages
            if (vi.pagesIntroTbrc != null && bPage <= vi.pagesIntroTbrc)
                bPage = vi.pagesIntroTbrc + 1;
            int ePage = imageInfoList.size();
            if (loc.evolnum != null && loc.evolnum.equals(vi.volumeNumber) && loc.epagenum != null)
                ePage = Math.min(loc.epagenum, ePage);
            if (!fairUse) {
                for (int seqNum = bPage; seqNum <= ePage; seqNum++) {
                    // imgSeqNum starts at 1
                    final String canvasUri = getCanvasUri(imageInfoList.get(seqNum - 1).filename, volumeId, seqNum);
                    subRange.addCanvas(canvasUri);
                }
            } else {
                final int firstUnaccessiblePage = AppConstants.FAIRUSE_PAGES_S + vi.pagesIntroTbrc + 1;
                final int lastUnaccessiblePage = imageInfoList.size() - AppConstants.FAIRUSE_PAGES_E;
                // first part: min(firstUnaccessiblePage+1,beginIndex) to
                // min(endIndex,firstUnaccessiblePage+1)
                for (int imgSeqNum = Math.min(firstUnaccessiblePage, bPage); imgSeqNum <= Math.min(ePage, firstUnaccessiblePage - 1); imgSeqNum++) {
                    final String canvasUri = getCanvasUri(imageInfoList.get(imgSeqNum - 1).filename, volumeId, imgSeqNum);
                    subRange.addCanvas(canvasUri);
                }
                // then copyright page, if either beginIndex or endIndex is
                // > FAIRUSE_PAGES_S+tbrcintro and < vi.totalPages-FAIRUSE_PAGES_E
                if ((bPage >= firstUnaccessiblePage && bPage <= lastUnaccessiblePage) || (ePage >= firstUnaccessiblePage && ePage <= lastUnaccessiblePage) || (bPage < firstUnaccessiblePage && ePage > lastUnaccessiblePage)) {
                    subRange.addCanvas(IIIFPresPrefix + "v:" + volumeId + "/canvas" + "/" + AppConstants.COPYRIGHT_PAGE_CANVAS_ID);
                }
                // last part: max(beginIndex,lastUnaccessiblePage) to
                // max(endIndex,lastUnaccessiblePage)
                for (int imgSeqNum = Math.max(lastUnaccessiblePage + 1, bPage); imgSeqNum <= Math.max(ePage, lastUnaccessiblePage); imgSeqNum++) {
                    final String canvasUri = getCanvasUri(imageInfoList.get(imgSeqNum - 1).filename, volumeId, imgSeqNum);
                    subRange.addCanvas(canvasUri);
                }
            }
        }
        if (part.parts != null) {
            for (final PartInfo subpart : part.parts) {
                addSubRangeToRange(m, subRange, id, subpart, vi, volumeId, imageInfoList, fairUse);
            }
        }
        m.addRange(subRange);
        r.addRange(rangeUri);
    }

    public static boolean pngOutput(final String filename) {
        final String ext = filename.substring(filename.length() - 4).toLowerCase();
        return (ext.equals(".tif") || ext.equals("tiff"));
    }

    public static final PropertyValue pngHint = new PropertyValue("png", "jpg");

    public static Canvas buildCanvas(final Identifier id, final Integer imgSeqNum, final List<ImageInfo> imageInfoList, final String volumeId, final VolumeInfo vi) {
        // imgSeqNum starts at 1
        final ImageInfo imageInfo = imageInfoList.get(imgSeqNum - 1);
        final PropertyValue label = getLabelForImage(imgSeqNum, vi);
        final String canvasUri = getCanvasUri(imageInfo.filename, volumeId, imgSeqNum);
        final Canvas canvas = new Canvas(canvasUri);
        canvas.setLabel(label);
        canvas.setWidth(imageInfo.width);
        canvas.setHeight(imageInfo.height);
        final String imageServiceUrl = getImageServiceUrl(imageInfo.filename, volumeId);
        ImageApiProfile profile = ImageApiProfile.LEVEL_ZERO;
        final Integer size = imageInfo.size;
        if (size != null && size > 2000000)
            profile = ImageApiProfile.LEVEL_ONE;
        final BDRCPresentationImageService imgServ = new BDRCPresentationImageService(imageServiceUrl, profile);
        final String imgUrl;
        if (pngOutput(imageInfo.filename)) {
            imgUrl = imageServiceUrl + "/full/max/0/default.png";
            imgServ.setPreferredFormats(pngHint);
        } else {
            imgUrl = imageServiceUrl + "/full/max/0/default.jpg";
        }
        imgServ.setHeight(imageInfo.height);
        imgServ.setWidth(imageInfo.width);
        final ImageContent img = new ImageContent(imgUrl);
        img.addService(imgServ);
        img.setWidth(imageInfo.width);
        img.setHeight(imageInfo.height);
        canvas.addImage(img);
        return canvas;
    }

    public static Integer getFileNameSeqNum(final List<ImageInfo> imageInfoList, final String filename) {
        int res = 1; // seqNum starts at 1
        for (final ImageInfo i : imageInfoList) {
            if (i.filename.equals(filename))
                return res;
            res += 1;
        }
        return null;
    }

    public static Canvas getCanvasForIdentifier(final Identifier id, final VolumeInfo vi, final int imgSeqNum, final String volumeId, final List<ImageInfo> imageInfoList) throws BDRCAPIException {
        if (id.getType() != Identifier.MANIFEST_ID || id.getSubType() != Identifier.MANIFEST_ID_VOLUMEID)
            throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "you cannot access this type of canvas");
        if (!vi.workId.startsWith(BDR))
            throw new BDRCAPIException(403, NO_ACCESS_ERROR_CODE, "you can only access BDRC volumes through this API");
        logger.info("building canvas for ID {}, imgSeqNum {}", id.getId(), imgSeqNum);
        final int imageTotal = imageInfoList.size();
        if (imgSeqNum < 1 || imgSeqNum > imageTotal)
            throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "you asked a canvas for an image number that is inferior to 1 or greater than the total number of images");
        return buildCanvas(id, imgSeqNum, imageInfoList, volumeId, vi);
    }
}
