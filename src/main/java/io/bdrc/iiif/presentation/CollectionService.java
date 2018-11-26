package io.bdrc.iiif.presentation;

import static io.bdrc.iiif.presentation.AppConstants.BDR_len;
import static io.bdrc.iiif.presentation.AppConstants.GENERIC_APP_ERROR_CODE;
import static io.bdrc.iiif.presentation.AppConstants.IIIFPresPrefix;
import static io.bdrc.iiif.presentation.AppConstants.IIIFPresPrefix_coll;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.digitalcollections.iiif.model.PropertyValue;
import de.digitalcollections.iiif.model.sharedcanvas.Collection;
import de.digitalcollections.iiif.model.sharedcanvas.Manifest;
import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.models.Identifier;
import io.bdrc.iiif.presentation.models.ItemInfo;
import io.bdrc.iiif.presentation.models.ItemInfo.VolumeInfoSmall;
import io.bdrc.iiif.presentation.models.WorkInfo;
import io.bdrc.iiif.presentation.models.WorkInfo.LangString;

public class CollectionService {

    private static final Logger logger = LoggerFactory.getLogger(CollectionService.class);
  //public static final List<ViewingHint> VIEW_HINTS=Arrays.asList(new ViewingHint[] { ViewingHint.CONTINUOUS});
    public static final String VIEW_HINTS= "continuous";

    public static String getPrefixedForm(final String id) {
        return "bdr:"+id.substring(BDR_len);
    }

    public static Collection getCollectionForIdentifier(final Identifier id,boolean continuous) throws BDRCAPIException {
        switch(id.getSubType()) {
        case Identifier.COLLECTION_ID_ITEM:
            return getCollectionForItem(id,continuous);
        case Identifier.COLLECTION_ID_WORK_OUTLINE:
            return getCollectionForOutline(id,continuous);
        default:
            throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "you cannot access this type of manifest yet");
        }
    }

    public static void addManifestsForLocation(final Collection c, final WorkInfo wi, final ItemInfo ii) {
        if (!wi.hasLocation)
            return;
        for (int i = wi.bvolnum ; i <= wi.evolnum ; i++) {
            VolumeInfoSmall vi = ii.getVolumeNumber(i);
            if (vi == null)
                continue;
            final int volumebPage = (i == wi.bvolnum) ? wi.bpagenum : 0;
            final int volumeePage = (i == wi.evolnum) ? wi.epagenum : -1;
            final StringBuilder sb = new StringBuilder();
            sb.append(IIIFPresPrefix+"v:"+vi.getPrefixedUri());
            if (volumebPage != 0 || volumeePage != -1) {
                sb.append("::");
                if (volumebPage != 0)
                    sb.append(volumebPage);
                sb.append("-");
                if (volumeePage != -1)
                    sb.append(volumeePage);
            }
            sb.append("/manifest");
            final Manifest m = new Manifest(sb.toString(), vi.toDisplay());
            c.addManifest(m);
        }
    }

    public static PropertyValue getLabels(String workId, WorkInfo wi) {
        final PropertyValue label = new PropertyValue();
        if (wi.labels == null || wi.labels.isEmpty()) {
            label.addValue(getPrefixedForm(workId));
            return label;
        }
        for (LangString ls : wi.labels) {
            if (ls.language != null)
                label.addValue(ManifestService.getLocaleFor(ls.language), ls.value);
            else
                label.addValue(ls.value);
        }
        return label;
    }

    public static Collection getCollectionForOutline(final Identifier id,boolean continuous) throws BDRCAPIException {
        final WorkInfo wi = WorkInfoService.getWorkInfo(id.getWorkId());
        final ItemInfo ii;
//        if (wi.access != AccessType.OPEN) {
//            throw new BDRCAPIException(403, NO_ACCESS_ERROR_CODE, "you cannot access this collection");
//        }
        logger.info("building outline collection for ID {}", id.getId());
        final Collection collection = new Collection(IIIFPresPrefix_coll+id.getId(), "Collection");
        collection.setAttribution(ManifestService.attribution);
        collection.addLicense("https://creativecommons.org/publicdomain/mark/1.0/");
        collection.addLogo("https://s3.amazonaws.com/bdrcwebassets/prod/iiif-logo.png");
        collection.setLabel(getLabels(id.getWorkId(), wi));
        if(continuous) {
            collection.setViewingHints(VIEW_HINTS);
        }
        if (wi.parts != null) {
            for (WorkInfo.PartInfo pi : wi.parts) {
                final String prefixedPartId = getPrefixedForm(pi.partId);
                final String collectionId = "wio:"+prefixedPartId;
                final Collection subcollection = new Collection(IIIFPresPrefix_coll+collectionId);
                subcollection.addLabel(prefixedPartId);
                if(continuous) {
                    subcollection.setViewingHints(VIEW_HINTS);
                }
                collection.addCollection(subcollection);
            }
        }
        if (id.getItemId() != null) {
            ii = ItemInfoService.getItemInfo(id.getItemId());
        } else if (wi.itemId != null) {
            ii = ItemInfoService.getItemInfo(wi.itemId);
        } else {
            // TODO: exception of wi.parts == null ? currently an empty collection is returned
            return collection;
        }
        addManifestsForLocation(collection, wi, ii);
        return collection;
    }

    public static Collection getCollectionForItem(final Identifier id,boolean continuous) throws BDRCAPIException {
        final ItemInfo ii = ItemInfoService.getItemInfo(id.getItemId());
//        if (ii.access != AccessType.OPEN) {
//            throw new BDRCAPIException(403, NO_ACCESS_ERROR_CODE, "you cannot access this collection");
//        }
        logger.info("building item collection for ID {}", id.getId());
        final Collection collection = new Collection(IIIFPresPrefix_coll+id.getId(), "Collection");
        collection.setAttribution(ManifestService.attribution);
        collection.addLicense("https://creativecommons.org/publicdomain/mark/1.0/");
        collection.addLogo("https://s3.amazonaws.com/bdrcwebassets/prod/iiif-logo.png");
        collection.addLabel(id.getItemId());
        if(continuous) {
            collection.setViewingHints(VIEW_HINTS);
        }
        for (ItemInfo.VolumeInfoSmall vi : ii.volumes) {
            final String manifestId = "v:"+vi.getPrefixedUri();
            final String volumeNumberStr = vi.toDisplay();
            final String manifestUrl = vi.iiifManifest == null ? IIIFPresPrefix+manifestId+"/manifest" : vi.iiifManifest;
            Manifest manifest = new Manifest(manifestUrl, volumeNumberStr);;
            if(continuous) {
                manifest = new Manifest(manifestUrl+"?continuous=true", volumeNumberStr);
            }
            collection.addManifest(manifest);
        }
        return collection;
    }
}
