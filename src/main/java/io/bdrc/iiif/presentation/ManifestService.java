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

import org.apache.commons.lang3.StringUtils;
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
import io.bdrc.iiif.presentation.resmodels.BVM;
import io.bdrc.iiif.presentation.resmodels.BVM.BVMImageInfo;
import io.bdrc.iiif.presentation.resmodels.BVM.BVMPaginationItem;
import io.bdrc.iiif.presentation.resmodels.BVM.BVMStatus;
import io.bdrc.iiif.presentation.resmodels.ImageGroupInfo;
import io.bdrc.iiif.presentation.resmodels.ImageInfoList;
import io.bdrc.iiif.presentation.resmodels.ImageInfoList.ImageInfo;
import io.bdrc.iiif.presentation.resmodels.InstanceOutline;
import io.bdrc.iiif.presentation.resmodels.LangString;
import io.bdrc.iiif.presentation.resmodels.Location;
import io.bdrc.iiif.presentation.resmodels.PartInfo;
import io.bdrc.iiif.presentation.resservices.BVMService;
import io.bdrc.iiif.presentation.resservices.ImageInfoListService;

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

    public static PropertyValue getDefaultLabelForImage(final int imageIndex, final ImageGroupInfo vi) {
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

    public static String getCanvasUri(final String filename, final String volumeId, final int seqNum) {
        // seqNum starts at 1
        return IIIFPresPrefix + "v:" + volumeId + "/canvas/" + filename;
    }

    public static ViewingDirection getViewingDirection(final ImageInfoList imageInfoList, final BVM bvm) {
        if (bvm != null && bvm.viewingDirection != null) {
            return bvm.viewingDirection.getIIIFViewingDirection();
        }
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

    public static Canvas addOneCanvas(final int imgSeqNum, final Identifier id, final ImageInfoList imageInfoList, final ImageGroupInfo vi,
            final String volumeId, final Sequence mainSeq, final BVMImageInfo bvmIi, final BVM bvm) {
        final ImageInfo imageInfo = imageInfoList.get(imgSeqNum - 1);
        final Integer size = imageInfo.size;
        if (size != null && size > 1500000)
            return null;
        final Canvas canvas = buildCanvas(id, imgSeqNum, imageInfoList, volumeId, vi, bvmIi, bvm);
        mainSeq.addCanvas(canvas);
        return canvas;
    }

    public static Canvas addCopyrightCanvas(final Sequence mainSeq, final String volumeId) {
        final String canvasUri = IIIFPresPrefix + "v:" + volumeId + "/canvas/" + AppConstants.COPYRIGHT_PAGE_CANVAS_ID;
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

    public static Sequence getSequenceFromBvm(final Identifier id, final ImageInfoList imageInfoList, final ImageGroupInfo vi, final String volumeId,
            final Integer ilBeginSeqNum, final Integer ilEndSeqNum, final boolean fairUse, final BVM bvm) throws BDRCAPIException {
        final Sequence mainSeq = new Sequence(IIIFPresPrefix + id.getId() + "/sequence/mainbvm");
        // all indices start at 1
        Canvas firstCanvas = null;
        // we're always in the not fairUse case with BVM
        // This handling of the null begin/end seqnum makes it better for cases where we
        // just want
        // all the images
        final Integer bvmBeginIndex;
        if (ilBeginSeqNum != null) {
            final String beginFileName = imageInfoList.get(ilBeginSeqNum - 1).filename;
            bvmBeginIndex = bvm.getDefaultBVMIndexForFilename(beginFileName, true);
            if (bvmBeginIndex == null)
                throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, "filename from image list not in bvm: " + beginFileName);
        } else {
            bvmBeginIndex = 0;
        }
        final Integer bvmEndIndex;
        if (ilEndSeqNum != null) {
            final String endFileName = imageInfoList.get(ilEndSeqNum - 1).filename;
            bvmEndIndex = bvm.getDefaultBVMIndexForFilename(endFileName, true);
            if (bvmEndIndex == null)
                throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, "filename from image list not in bvm: " + endFileName);
        } else {
            bvmEndIndex = bvm.getDefaultImageList().size() - 1;
        }
        final List<BVMImageInfo> bvmIil = bvm.getDefaultImageList();
        for (int bvmImgIdx = bvmBeginIndex; bvmImgIdx <= bvmEndIndex; bvmImgIdx++) {
            final BVMImageInfo bvmIi = bvmIil.get(bvmImgIdx);
            if (bvmIi.filename == null || bvmIi.filename.isEmpty()) {
                // empty canvas
                final String canvasUri = IIIFPresPrefix + "v:" + volumeId + "/canvas/missing-bvmidx-" + bvmImgIdx;
                final Canvas thisCanvas = new Canvas(canvasUri);
                // this width/heigth is a bit random...
                thisCanvas.setWidth(AppConstants.COPYRIGHT_PAGE_W);
                thisCanvas.setHeight(AppConstants.COPYRIGHT_PAGE_H);
                final PropertyValue label = getLabelFromBVMImageInfo(bvmIi, bvm, true);
                if (label != null)
                    thisCanvas.setLabel(label);
                if (firstCanvas == null)
                    firstCanvas = thisCanvas;
                mainSeq.addCanvas(thisCanvas);
            } else {
                final Integer iiLidx = imageInfoList.getIdxFromFilename(bvmIi.filename);
                if (iiLidx == null) {
                    throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, "filename from bvm not in imagelist: " + bvmIi.filename);
                }
                final Canvas thisCanvas = addOneCanvas(iiLidx + 1, id, imageInfoList, vi, volumeId, mainSeq, bvmIi, bvm);
                if (firstCanvas == null)
                    firstCanvas = thisCanvas;
            }
        }
        mainSeq.setStartCanvas(firstCanvas.getIdentifier());
        return mainSeq;
    }

    public static Sequence getSequenceFrom(boolean isAdmin, final Identifier id, final ImageInfoList imageInfoList, final ImageGroupInfo vi,
            final String volumeId, Integer beginIndex, Integer endIndex, final boolean fairUse, final BVM bvm) throws BDRCAPIException {
        final Sequence mainSeq = new Sequence(IIIFPresPrefix + id.getId() + "/sequence/main");
        if (bvm != null && !fairUse) {
            return getSequenceFromBvm(id, imageInfoList, vi, volumeId, beginIndex, endIndex, fairUse, bvm);
        }
        if (beginIndex == null)
            beginIndex = 1 + vi.pagesIntroTbrc;
        if (endIndex == null)
            endIndex = imageInfoList.size();
        if (endIndex <= beginIndex) {
            // this is the case where the manifest would be empty, we return a 404 instead
            throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "manifest would be empty");
        }
        // all indices start at 1
        mainSeq.setViewingDirection(getViewingDirection(imageInfoList, bvm));
        Canvas firstCanvas = null;
        final int totalPages = imageInfoList.size();
        if (!fairUse || isAdmin) {
            for (int imgSeqNum = beginIndex; imgSeqNum <= endIndex; imgSeqNum++) {
                final Canvas thisCanvas = addOneCanvas(imgSeqNum, id, imageInfoList, vi, volumeId, mainSeq, null, null);
                if (firstCanvas == null)
                    firstCanvas = thisCanvas;
            }
        } else {
            final int firstUnaccessiblePage = AppConstants.FAIRUSE_PAGES_S + vi.pagesIntroTbrc + 1;
            final int lastUnaccessiblePage = totalPages - AppConstants.FAIRUSE_PAGES_E;
            // first part: min(firstUnaccessiblePage+1,beginIndex) to
            // min(endIndex,firstUnaccessiblePage+1)
            for (int imgSeqNum = Math.min(firstUnaccessiblePage, beginIndex); imgSeqNum <= Math.min(endIndex,
                    firstUnaccessiblePage - 1); imgSeqNum++) {
                final Canvas thisCanvas = addOneCanvas(imgSeqNum, id, imageInfoList, vi, volumeId, mainSeq, null, null);
                if (firstCanvas == null)
                    firstCanvas = thisCanvas;
            }
            // then copyright page, if either beginIndex or endIndex is
            // > FAIRUSE_PAGES_S+tbrcintro and < vi.totalPages-FAIRUSE_PAGES_E
            if ((beginIndex >= firstUnaccessiblePage && beginIndex <= lastUnaccessiblePage)
                    || (endIndex >= firstUnaccessiblePage && endIndex <= lastUnaccessiblePage)
                    || (beginIndex < firstUnaccessiblePage && endIndex > lastUnaccessiblePage)) {
                final Canvas thisCanvas = addCopyrightCanvas(mainSeq, volumeId);
                if (firstCanvas == null)
                    firstCanvas = thisCanvas;
            }
            // last part: max(beginIndex,lastUnaccessiblePage) to
            // max(endIndex,lastUnaccessiblePage)
            for (int imgSeqNum = Math.max(lastUnaccessiblePage + 1, beginIndex); imgSeqNum <= Math.max(endIndex, lastUnaccessiblePage); imgSeqNum++) {
                final Canvas thisCanvas = addOneCanvas(imgSeqNum, id, imageInfoList, vi, volumeId, mainSeq, null, null);
                if (firstCanvas == null)
                    firstCanvas = thisCanvas;
            }
        }
        if (firstCanvas != null)
            mainSeq.setStartCanvas(firstCanvas.getIdentifier());
        return mainSeq;
    }

    public static String getVolNumLabelSuffix(final String language, final Integer volnum) {
        switch (language) {
        case "bo-x-ewts":
            return "_pod/_" + Integer.toString(volnum);
        case "bo":
            return " པོད། " + StringUtils.replaceChars(Integer.toString(volnum), "0123456789", "༠༡༢༣༤༥༦༧༨༩");
        case "en":
            return " (vol. " + Integer.toString(volnum) + ")";
        default:
            return " (" + Integer.toString(volnum) + ")";
        }
    }

    public static PropertyValue getVolNumPV(final Integer volnum) {
        final PropertyValue res = new PropertyValue();
        res.addValue(ManifestService.getLocaleFor("en"), "volume " + volnum);
        res.addValue(ManifestService.getLocaleFor("bo-x-ewts"), "pod/_" + volnum);
        return res;
    }

    public static PropertyValue getPVforLS(final List<LangString> lslist, final Integer volnum) {
        final PropertyValue res = new PropertyValue();
        for (final LangString ls : lslist) {
            final String valueSuffix;
            if (volnum != null) {
                valueSuffix = getVolNumLabelSuffix(ls.language, volnum);
            } else {
                valueSuffix = "";
            }
            if (ls.language != null) {
                res.addValue(ManifestService.getLocaleFor(ls.language), ls.value + valueSuffix);
            } else {
                res.addValue(ls.value + valueSuffix);
            }
        }
        return res;
    }

    public static Manifest getManifestForIdentifier(boolean isAdmin, final Identifier id, final ImageGroupInfo vi, boolean continuous,
            final String volumeId, final boolean fairUse, final PartInfo rootPart) throws BDRCAPIException {
        if (id.getType() != Identifier.MANIFEST_ID || (id.getSubType() != Identifier.MANIFEST_ID_VOLUMEID
                && id.getSubType() != Identifier.MANIFEST_ID_WORK_IN_VOLUMEID && id.getSubType() != Identifier.MANIFEST_ID_VOLUMEID_OUTLINE
                && id.getSubType() != Identifier.MANIFEST_ID_WORK_IN_VOLUMEID_OUTLINE)) {
            throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "you cannot access this type of manifest yet");
        }
        if (!vi.imageInstanceUri.startsWith(BDR)) {
            throw new BDRCAPIException(403, NO_ACCESS_ERROR_CODE, "you can only access BDRC volumes through this API");
        }
        logger.info("building manifest for ID {}", id.getId());
        logger.debug("rootPart: {}", rootPart);
        final String imageInstanceLocalName = vi.imageInstanceUri.substring(BDR_len);
        ImageInfoList imageInfoList;
        BVM bvm = null;
        try {
            imageInfoList = ImageInfoListService.Instance.getAsync(imageInstanceLocalName, vi.imageGroup).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, e);
        }
        try {
            bvm = BVMService.Instance.getAsync(vi.imageGroup).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, e);
        }
        // TODO: have a a way to test BVMs? maybe a query url?
        if (bvm != null && bvm.status != BVMStatus.released) {
            bvm = null;
        }
        final Manifest manifest = new Manifest(IIIFPresPrefix + id.getId() + "/manifest");
        manifest.setAttribution(attribution);
        if (vi.license != null)
            manifest.addLicense(vi.license.getIIIFUri());
        manifest.addLogo(IIIF_IMAGE_PREFIX + "static::logo.png/full/max/0/default.png");
        if (id.getSubType() == Identifier.MANIFEST_ID_VOLUMEID || id.getSubType() == Identifier.MANIFEST_ID_VOLUMEID_OUTLINE) {
            // if label is for the whole volume: first bvm, if none then image group info,
            // if not the volume number:
            if (bvm != null && bvm.label != null && !bvm.label.isEmpty()) {
                manifest.setLabel(getPVforLS(bvm.label, vi.volumeNumber));
            } else if (vi != null && vi.labels != null && !vi.labels.isEmpty()) {
                manifest.setLabel(getPVforLS(vi.labels, vi.volumeNumber));
            } else {
                manifest.setLabel(getVolNumPV(vi.volumeNumber));
            }
        } else {
            if (rootPart != null && rootPart.labels != null && !rootPart.labels.isEmpty()) {
                manifest.setLabel(getPVforLS(rootPart.labels, vi.volumeNumber));
            } else {
                manifest.setLabel(getVolNumPV(vi.volumeNumber));
            }
        }
        // TODO: when there's a BVM, perhaps we should only rely on it to display/not
        // display images,
        // including scan request pages...
        int nbPagesIntro = vi.pagesIntroTbrc;
        Integer bPage;
        Integer ePage;
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
                if (location.bvolnum.equals(vi.volumeNumber)  && location.bpagenum != null) {
                    bPage = location.bpagenum;
                    if (bPage <= nbPagesIntro)
                        bPage = nbPagesIntro + 1;
                }
                if (location.evolnum < vi.volumeNumber)
                    throw new BDRCAPIException(404, NO_ACCESS_ERROR_CODE, "the work you asked ends before this volume");
                // if evolnum > vi.volumeNumber, we already have bPage correctly set to
                // totalPages
                if (vi.volumeNumber.equals(location.evolnum) && location.epagenum != -1)
                    ePage = location.epagenum;
            }
        } else {
            bPage = id.getBPageNum();
            ePage = id.getEPageNum();
        }
        logger.debug("computed: {}-{}", bPage, ePage);
        final Sequence mainSeq = getSequenceFrom(isAdmin, id, imageInfoList, vi, volumeId, bPage, ePage, fairUse, bvm);
        if (continuous) {
            mainSeq.setViewingHints(VIEWING_HINTS);
        }
        // PDF / zip download
        final List<OtherContent> oc = getRenderings(volumeId, bPage, ePage);
        manifest.setRenderings(oc);
        if (id.getSubType() == Identifier.MANIFEST_ID_VOLUMEID_OUTLINE || id.getSubType() == Identifier.MANIFEST_ID_WORK_IN_VOLUMEID_OUTLINE) {
            addRangesToManifest(isAdmin, manifest, id, vi, volumeId, fairUse, imageInfoList, rootPart, bvm);
        }
        manifest.addSequence(mainSeq);
        return manifest;
    }

    public static List<OtherContent> getRenderings(final String volumeId, Integer bPage, Integer ePage) {
        if (bPage == null)
            bPage = 1;
        final String fullId = volumeId + "::" + bPage + "-" + (ePage != null ? ePage : "");
        final OtherContent oct = new OtherContent(PDF_URL_PREFIX + "v:" + fullId, "application/pdf");
        oct.setLabel(new PropertyValue("Download as PDF"));
        final OtherContent oct1 = new OtherContent(ZIP_URL_PREFIX + "v:" + fullId, "application/zip");
        oct1.setLabel(new PropertyValue("Download as ZIP"));
        final ArrayList<OtherContent> ct = new ArrayList<>();
        ct.add(oct);
        ct.add(oct1);
        return ct;
    }

    public static void addRangesToManifest(boolean isAdmin, final Manifest m, final Identifier id, final ImageGroupInfo vi, final String volumeId,
            final boolean fairUse, final ImageInfoList imageInfoList, final PartInfo rootPi, final BVM bvm) throws BDRCAPIException {
        if (rootPi == null)
            return;
        final PartInfo volumeRoot = InstanceOutline.getRootPiForVolumeR(rootPi, vi.volumeNumber);
        if (volumeRoot == null)
            return;
        final Range r = new Range(IIIFPresPrefix + "v:" + id.getImageGroupId() + "/range/top", "Table of Contents");
        r.setViewingHints("top");
        for (final PartInfo part : volumeRoot.parts) {
            addSubRangeToRange(isAdmin, m, r, id, part, vi, volumeId, imageInfoList, fairUse, bvm);
        }
        m.addRange(r);
    }

    public static void addSubRangeToRange(boolean isAdmin, final Manifest m, final Range r, final Identifier id, final PartInfo part,
            final ImageGroupInfo vi, final String volumeId, final ImageInfoList imageInfoList, final boolean fairUse, BVM bvm)
            throws BDRCAPIException {
        // do not add ranges where there is no location nor subparts
        if (part.location == null && part.parts == null)
            return;
        final String rangeUri = IIIFPresPrefix + "v:" + volumeId + "/range/w:" + part.partQname;
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
            if (!fairUse || isAdmin) {
                if (bvm == null) {
                    for (int seqNum = bPage; seqNum <= ePage; seqNum++) {
                        // imgSeqNum starts at 1
                        final String canvasUri = getCanvasUri(imageInfoList.get(seqNum - 1).filename, volumeId, seqNum);
                        subRange.addCanvas(canvasUri);
                    }
                } else {
                    final String beginFileName = imageInfoList.get(bPage - 1).filename;
                    final String endFileName = imageInfoList.get(ePage - 1).filename;
                    final Integer bvmBeginIndex = bvm.getDefaultBVMIndexForFilename(beginFileName, true);
                    final Integer bvmEndIndex = bvm.getDefaultBVMIndexForFilename(endFileName, true);
                    if (bvmBeginIndex == null || bvmEndIndex == null) {
                        throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE,
                                "filenames from image list not in bvm: " + beginFileName + " or " + endFileName);
                    }
                    final List<BVMImageInfo> bvmIil = bvm.getDefaultImageList();
                    for (int bvmImgIdx = bvmBeginIndex; bvmImgIdx <= bvmEndIndex; bvmImgIdx++) {
                        final BVMImageInfo bvmIi = bvmIil.get(bvmImgIdx);
                        if (bvmIi.filename == null || bvmIi.filename.isEmpty()) {
                            // empty canvas
                            final String canvasUri = IIIFPresPrefix + "v:" + volumeId + "/canvas/missing-bvmidx-" + bvmImgIdx;
                            subRange.addCanvas(canvasUri);
                        } else {
                            final Integer iiLidx = imageInfoList.getIdxFromFilename(bvmIi.filename);
                            if (iiLidx == null) {
                                throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, "filename from bvm not in imagelist: " + bvmIi.filename);
                            }
                            final String canvasUri = getCanvasUri(bvmIi.filename, volumeId, iiLidx);
                            subRange.addCanvas(canvasUri);
                        }
                    }
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
                if ((bPage >= firstUnaccessiblePage && bPage <= lastUnaccessiblePage)
                        || (ePage >= firstUnaccessiblePage && ePage <= lastUnaccessiblePage)
                        || (bPage < firstUnaccessiblePage && ePage > lastUnaccessiblePage)) {
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
                addSubRangeToRange(isAdmin, m, subRange, id, subpart, vi, volumeId, imageInfoList, fairUse, bvm);
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

    public static PropertyValue getLabelFromBVMImageInfo(final BVMImageInfo bvmIi, final BVM bvm, final boolean missing) {
        // TODO: convert pagination in Tibetan?
        final BVMPaginationItem paginationItem = bvmIi.getDefaultPaginationValue(bvm);
        if (paginationItem == null || paginationItem.value == null || paginationItem.value.isEmpty()) {
            if (missing) {
                final PropertyValue res = new PropertyValue();
                res.addValue(ManifestService.getLocaleFor("en"), "missing");
                return res;
            }
            return null;
        }
        final PropertyValue res = new PropertyValue();
        if (missing) {
            res.addValue(ManifestService.getLocaleFor("en"), paginationItem.value + " (missing)");
        } else {
            res.addValue(paginationItem.value);
        }
        return res;
    }

    public static Canvas buildCanvas(final Identifier id, final Integer imgSeqNum, final ImageInfoList imageInfoList, final String volumeId,
            final ImageGroupInfo vi, final BVMImageInfo bvmIi, final BVM bvm) {
        // imgSeqNum starts at 1
        final ImageInfo imageInfo = imageInfoList.get(imgSeqNum - 1);
        final String canvasUri = getCanvasUri(imageInfo.filename, volumeId, imgSeqNum);
        final Canvas canvas = new Canvas(canvasUri);
        if (bvmIi != null) {
            final PropertyValue label = getLabelFromBVMImageInfo(bvmIi, bvm, false);
            if (label != null)
                canvas.setLabel(label);
        } else {
            canvas.setLabel(getDefaultLabelForImage(imgSeqNum, vi));
        }
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

    public static Canvas getCanvasForIdentifier(final Identifier id, final ImageGroupInfo vi, final int imgSeqNum, final String volumeId,
            final ImageInfoList imageInfoList) throws BDRCAPIException {
        // TODO: get the BVM if available, so that we can pass it as an argument to
        // buildCanvas()
        if (id.getType() != Identifier.MANIFEST_ID || id.getSubType() != Identifier.MANIFEST_ID_VOLUMEID)
            throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "you cannot access this type of canvas");
        if (!vi.instanceUri.startsWith(BDR))
            throw new BDRCAPIException(403, NO_ACCESS_ERROR_CODE, "you can only access BDRC volumes through this API");
        logger.info("building canvas for ID {}, imgSeqNum {}", id.getId(), imgSeqNum);
        final int imageTotal = imageInfoList.size();
        if (imgSeqNum < 1 || imgSeqNum > imageTotal)
            throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE,
                    "you asked a canvas for an image number that is inferior to 1 or greater than the total number of images");
        return buildCanvas(id, imgSeqNum, imageInfoList, volumeId, vi, null, null);
    }
}
