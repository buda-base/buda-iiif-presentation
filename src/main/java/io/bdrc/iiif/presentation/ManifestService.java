package io.bdrc.iiif.presentation;

import static io.bdrc.iiif.presentation.AppConstants.BDR;
import static io.bdrc.iiif.presentation.AppConstants.BDR_len;
import static io.bdrc.iiif.presentation.AppConstants.GENERIC_APP_ERROR_CODE;
import static io.bdrc.iiif.presentation.AppConstants.IIIFPresPrefix;
import static io.bdrc.iiif.presentation.AppConstants.IIIF_IMAGE_PREFIX;
import static io.bdrc.iiif.presentation.AppConstants.PDF_URL_PREFIX;
import static io.bdrc.iiif.presentation.AppConstants.NO_ACCESS_ERROR_CODE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.digitalcollections.iiif.model.ImageContent;
import de.digitalcollections.iiif.model.OtherContent;
import de.digitalcollections.iiif.model.PropertyValue;
import de.digitalcollections.iiif.model.enums.ViewingDirection;
import de.digitalcollections.iiif.model.image.ImageApiProfile;
import de.digitalcollections.iiif.model.image.ImageService;
import de.digitalcollections.iiif.model.sharedcanvas.Canvas;
import de.digitalcollections.iiif.model.sharedcanvas.Manifest;
import de.digitalcollections.iiif.model.sharedcanvas.Range;
import de.digitalcollections.iiif.model.sharedcanvas.Sequence;
import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.models.AccessType;
import io.bdrc.iiif.presentation.models.Identifier;
import io.bdrc.iiif.presentation.models.ImageInfo;
import io.bdrc.iiif.presentation.models.LangString;
import io.bdrc.iiif.presentation.models.Location;
import io.bdrc.iiif.presentation.models.PartInfo;
import io.bdrc.iiif.presentation.models.VolumeInfo;


public class ManifestService {

    private static final Logger logger = LoggerFactory.getLogger(ManifestService.class);
    
    public static final Map<String, Locale> locales = new HashMap<>();
    public static final PropertyValue attribution = new PropertyValue();
    //public static final List<ViewingHint> VIEW_HINTS=Arrays.asList(new ViewingHint[] { ViewingHint.CONTINUOUS});
    public static final String VIEWING_HINTS= "continuous";
    static {
        attribution.addValue(getLocaleFor("en"), "Buddhist Digital Resource Center");
        attribution.addValue(getLocaleFor("bo"), "ནང་བསྟན་དཔེ་ཚོགས་ལྟེ་གནས།");
        attribution.addValue(getLocaleFor("zh"), "佛教数字资源中心(BDRC)");
    }

    public static Locale getLocaleFor(String lt) {
        return locales.computeIfAbsent(lt, x -> Locale.forLanguageTag(lt));
    }
    
    public static PropertyValue getPropForLabels(List<LangString> labels) {
        if (labels == null) return null;
        final PropertyValue res = new PropertyValue();
        if (labels.size() == 1) { 
            // no lang tag if we have only one lang, that might be changed later on
            // (it's mostly to work around a bug in Mirador)
            res.addValue(labels.get(0).value);
            return res;
        }
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

    public static String getLabelForImage(final int imageIndex) {
        // indices start at 1
        if (imageIndex < 3)
            return "tbrc-"+imageIndex;
        return "p. "+(imageIndex-2);
    }

    public static String getImageServiceUrl(final String filename, final Identifier id) {
        return IIIF_IMAGE_PREFIX+id.getVolumeId()+"::"+filename;
    }

    // warning: not all calls to this function profile the filename argument
    public static String getCanvasUri(final String filename, final Identifier id, final int seqNum) {
        // seqNum starts at 1
        //return IIIFPresPrefix+id.getVolumeId()+"::"+filename+"/canvas";
        return IIIFPresPrefix+"v:"+id.getVolumeId()+"/canvas"+"/"+seqNum;
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
    
    public static Sequence getSequenceFrom(final Identifier id, final List<ImageInfo> imageInfoList) throws BDRCAPIException {
        final Sequence mainSeq = new Sequence(IIIFPresPrefix+id.getId()+"/sequence/main");
        final int imageTotal = imageInfoList.size();
        // all indices start at 1
        final int beginIndex = (id.getBPageNum() == null) ? 1 : id.getBPageNum();
        if (beginIndex > imageTotal) {
            throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "you asked a manifest for an image number that is greater than the total number of images");
        }
        Integer ePageNum = id.getEPageNum();
        int endIndex = imageTotal;
        if (ePageNum != null) {
            if (ePageNum > imageTotal) {
                ePageNum = imageTotal;
                logger.warn("user asked manifest for id {}, which has an end image ({}) larger than the total number of images ({})", id, ePageNum, imageTotal-1);
            }
            endIndex = ePageNum;
        }
        mainSeq.setViewingDirection(getViewingDirection(imageInfoList));
        for (int imgSeqNum = beginIndex ; imgSeqNum <= endIndex ; imgSeqNum++) {
            final Canvas canvas = buildCanvas(id, imgSeqNum, imageInfoList);
            mainSeq.addCanvas(canvas);
            if (imgSeqNum == beginIndex) {
                mainSeq.setStartCanvas(canvas.getIdentifier());
            }
        }
        return mainSeq;
    }

    public static Manifest getManifestForIdentifier(final Identifier id, final VolumeInfo vi, boolean continuous) throws BDRCAPIException {
        if (id.getType() != Identifier.MANIFEST_ID || (
                id.getSubType() != Identifier.MANIFEST_ID_VOLUMEID
                && id.getSubType() != Identifier.MANIFEST_ID_VOLUMEID_OUTLINE)) {
            throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "you cannot access this type of manifest yet");
        }
        if (!vi.workId.startsWith(BDR)) {
            throw new BDRCAPIException(403, NO_ACCESS_ERROR_CODE, "you can only access BDRC volumes through this API");
        }
        final String workLocalId = vi.workId.substring(BDR_len);
        logger.info("building manifest for ID {}", id.getId());
        final List<ImageInfo> imageInfoList = ImageInfoListService.getImageInfoList(workLocalId, vi.imageGroup);
        final Manifest manifest = new Manifest(IIIFPresPrefix+id.getId()+"/manifest", "Volume "+vi.volumeNumber);
        manifest.setAttribution(attribution);
        manifest.addLicense("https://creativecommons.org/publicdomain/mark/1.0/");
        manifest.addLogo("https://s3.amazonaws.com/bdrcwebassets/prod/iiif-logo.png");
        manifest.addLabel(id.getVolumeId());
        final Sequence mainSeq = getSequenceFrom(id, imageInfoList);
        mainSeq.setViewingDirection(ViewingDirection.TOP_TO_BOTTOM);
        if (continuous) {
            mainSeq.setViewingHints(VIEWING_HINTS);
        }
        // PDF / zip download
        int bPage = id.getBPageNum() == null ? 1 : id.getBPageNum().intValue(); 
        int ePage = id.getEPageNum() == null ? vi.totalPages : id.getEPageNum().intValue();
        final List<OtherContent> oc = getRenderings(id.getVolumeId(), bPage, ePage); 
        manifest.setRenderings(oc);
        if (id.getSubType() == Identifier.MANIFEST_ID_VOLUMEID_OUTLINE) {
            addRangesToManifest(manifest, id, vi);
        }
        manifest.addSequence(mainSeq);
        return manifest;
    }

    public static List<OtherContent> getRenderings(final String volumeId, final int bPage, final int ePage) {
        final String fullId = volumeId+"::"+bPage+"-"+ePage;
        final OtherContent oct = new OtherContent(PDF_URL_PREFIX+"v:"+fullId,"application/pdf");
        oct.setLabel(new PropertyValue("Download as PDF"));
        final OtherContent oct1 = new OtherContent(PDF_URL_PREFIX+"v:"+fullId,"application/zip");
        oct1.setLabel(new PropertyValue("Download as ZIP"));
        final ArrayList<OtherContent> ct = new ArrayList<>();
        ct.add(oct);
        ct.add(oct1);
        return ct;
    }
    
    public static void addRangesToManifest(final Manifest m, final Identifier id, final VolumeInfo vi) throws BDRCAPIException {
        if (vi.partInfo == null) {
            return;
        }
        final Range r = new Range(IIIFPresPrefix+"vo:"+id.getVolumeId()+"/range/top", "Table of Contents");
        r.setViewingHints("top");
        for (final PartInfo part : vi.partInfo) {
            addSubRangeToRange(m, r, id, part, vi);
        }
        m.addRange(r);
    }
    
    public static void addSubRangeToRange(final Manifest m, final Range r, final Identifier id, final PartInfo part, final VolumeInfo vi) throws BDRCAPIException {
        final String rangeUri = IIIFPresPrefix+"vo:"+id.getVolumeId()+"/range/w:"+part.partId;
        final Range subRange = new Range(rangeUri);
        final PropertyValue labels = getPropForLabels(part.labels);
        subRange.setLabel(labels);
        if (part.location != null) {
            final Location loc = part.location;
            int bPage = 1;
            if (loc.bvolnum == vi.volumeNumber && loc.bpagenum != null)
                bPage = loc.bpagenum;
            // ignoring the tbrc pages
            if (vi.pagesIntroTbrc != null && bPage <= vi.pagesIntroTbrc)
                bPage = vi.pagesIntroTbrc+1;
            int ePage = vi.totalPages;
            if (loc.evolnum != null && loc.evolnum == vi.volumeNumber && loc.epagenum != null)
                ePage = loc.epagenum;
            for (int seqNum = bPage ; seqNum <= ePage ; seqNum ++) {
                // TODO: add the first argument (filename)
                final String canvasUri = getCanvasUri(null, id, seqNum);
                subRange.addCanvas(canvasUri);
            }
        }
        if (part.subparts != null) {
            for (final PartInfo subpart : part.subparts) {
                addSubRangeToRange(m, subRange, id, subpart, vi);
            }
        }
        m.addRange(subRange);
        r.addRange(rangeUri);
    }
    
    public static boolean pngOutput(final String filename) {
        final String ext = filename.substring(filename.length()-4).toLowerCase();
        return (ext.equals(".tif") || ext.equals("tiff"));
    }
    
    public static Canvas buildCanvas(final Identifier id, final Integer imgSeqNum, final List<ImageInfo> imageInfoList) {
        // imgSeqNum starts at 1
        final ImageInfo imageInfo = imageInfoList.get(imgSeqNum-1);
        final String label = getLabelForImage(imgSeqNum);
        final String canvasUri = getCanvasUri(imageInfo.filename, id, imgSeqNum);
        final Canvas canvas = new Canvas(canvasUri, label);
        canvas.setWidth(imageInfo.width);
        canvas.setHeight(imageInfo.height);
        final String imageServiceUrl = getImageServiceUrl(imageInfo.filename, id);
        //canvas.addIIIFImage(imageServiceUrl, ImageApiProfile.LEVEL_ONE);
        final ImageService imgServ = new ImageService(imageServiceUrl, ImageApiProfile.LEVEL_ZERO);
        final String imgUrl;
        if (pngOutput(imageInfo.filename)) {
            imgUrl = imageServiceUrl+"full/full/0/default.png";
        } else {
            imgUrl = imageServiceUrl+"full/full/0/default.jpg";
        }
        final ImageContent img = new ImageContent(imgUrl);
        img.addService(imgServ);
        img.setWidth(imageInfo.width);
        img.setHeight(imageInfo.height);
        canvas.addImage(img);
        return canvas;
    }
    
    public static Canvas getCanvasForIdentifier(final Identifier id, final VolumeInfo vi, final String imgSeqNumS) throws BDRCAPIException {
        final Integer imgSeqNum;
        try {
            imgSeqNum = Integer.parseInt(imgSeqNumS);
        } catch (NumberFormatException e) {
            throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "cannot understand final part of URL");
        }
        if (id.getType() != Identifier.MANIFEST_ID || id.getSubType() != Identifier.MANIFEST_ID_VOLUMEID) {
            throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "you cannot access this type of manifest yet");
        }
        if (vi.access != AccessType.OPEN) {
            throw new BDRCAPIException(403, NO_ACCESS_ERROR_CODE, "you cannot access this volume");
        }
        if (!vi.workId.startsWith(BDR)) {
            throw new BDRCAPIException(403, NO_ACCESS_ERROR_CODE, "you can only access BDRC volumes through this API");
        }
        final String workLocalId = vi.workId.substring(BDR_len);
        logger.info("building canvas for ID {}, imgSeqNum {}", id.getId(), imgSeqNum);
        final List<ImageInfo> imageInfoList = ImageInfoListService.getImageInfoList(workLocalId, vi.imageGroup);
        final int imageTotal = imageInfoList.size();
        if (imgSeqNum < 1 || imgSeqNum > imageTotal) {
            throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "you asked a canvas for an image number that is inferior to 1 or greater than the total number of images");
        }
        return buildCanvas(id, imgSeqNum, imageInfoList);
    }
}
