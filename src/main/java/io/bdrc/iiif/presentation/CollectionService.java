package io.bdrc.iiif.presentation;

import de.digitalcollections.iiif.model.sharedcanvas.Collection;
import de.digitalcollections.iiif.model.sharedcanvas.Manifest;
import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.models.AccessType;
import io.bdrc.iiif.presentation.models.Identifier;
import io.bdrc.iiif.presentation.models.ItemInfo;
import io.bdrc.iiif.presentation.models.WorkInfo;

import static io.bdrc.iiif.presentation.AppConstants.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CollectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(ManifestService.class);
    
    public static String getPrefixedForm(String id) {
        return "bdr:"+id.substring(BDR_len);
    }
    
    public static Collection getCollectionForIdentifier(final Identifier id) throws BDRCAPIException {
        switch(id.getSubType()) {
        case Identifier.COLLECTION_ID_ITEM:
            return getCollectionForItem(id);
        case Identifier.COLLECTION_ID_WORK_OUTLINE:
            return null;
        default:
            throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "you cannot access this type of manifest yet");
        }
    }

    public static Collection getCollectionForOutline(final Identifier id) throws BDRCAPIException {
        final WorkInfo wi = WorkInfoService.getWorkInfo(id.getWorkId());
//        if (ii.access != AccessType.OPEN) {
//            throw new BDRCAPIException(403, NO_ACCESS_ERROR_CODE, "you cannot access this collection");
//        }
        final ItemInfo ii;
        if (id.getItemId() != null) {
            ii = ItemInfoService.getItemInfo(id.getItemId());
        } else if (wi.itemId != null) {
            ii = ItemInfoService.getItemInfo(wi.itemId);
        } else {
            ii = null;
        }
        logger.info("building outline collection for ID {}", id.getId());
        final Collection collection = new Collection(IIIFPresPrefix_coll+id.getId(), "Collection");
        collection.setAttribution(ManifestService.attribution);
        collection.addLicense("https://creativecommons.org/publicdomain/mark/1.0/");
        collection.addLogo("https://eroux.fr/logo.png");
        for (WorkInfo.PartInfo pi : wi.parts) {
            final String prefixedPartId = getPrefixedForm(pi.partId);
            final String collectionId = "wio:"+prefixedPartId;
            final Collection subcollection = new Collection(IIIFPresPrefix_coll+collectionId);
            collection.addCollection(subcollection);
            //final Manifest manifest = new Manifest(IIIFPresPrefix+"/manifest")
        }
        return collection;
    }
    
    public static Collection getCollectionForItem(final Identifier id) throws BDRCAPIException {

        final ItemInfo ii = ItemInfoService.getItemInfo(id.getItemId());
        if (ii.access != AccessType.OPEN) {
            throw new BDRCAPIException(403, NO_ACCESS_ERROR_CODE, "you cannot access this collection");
        }
        logger.info("building item collection for ID {}", id.getId());
        final Collection collection = new Collection(IIIFPresPrefix_coll+id.getId(), "Collection");
        collection.setAttribution(ManifestService.attribution);
        collection.addLicense("https://creativecommons.org/publicdomain/mark/1.0/");
        collection.addLogo("https://eroux.fr/logo.png");
        for (ItemInfo.VolumeInfoSmall vi : ii.volumes) {
            final String prefixedVolumeId = getPrefixedForm(vi.volumeId);
            final String manifestId = "v:"+prefixedVolumeId;
            final String volumeNumberStr;
            if (vi.volumeNumber == null)
                volumeNumberStr = prefixedVolumeId;
            else
                volumeNumberStr = "Volume "+vi.volumeNumber;
            Manifest manifest = new Manifest(IIIFPresPrefix+manifestId+"/manifest", volumeNumberStr);
            collection.addManifest(manifest);
        }
        return collection;
    }
}
