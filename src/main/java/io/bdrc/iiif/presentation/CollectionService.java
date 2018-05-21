package io.bdrc.iiif.presentation;

import de.digitalcollections.iiif.model.sharedcanvas.Collection;
import de.digitalcollections.iiif.model.sharedcanvas.Manifest;
import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.models.Identifier;
import io.bdrc.iiif.presentation.models.ItemInfo;
import io.bdrc.iiif.presentation.models.ItemInfo.VolumeInfoSmall;
import io.bdrc.iiif.presentation.models.WorkInfo;

import static io.bdrc.iiif.presentation.AppConstants.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CollectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(CollectionService.class);
    
    public static String getPrefixedForm(final String id) {
        return "bdr:"+id.substring(BDR_len);
    }
    
    public static Collection getCollectionForIdentifier(final Identifier id) throws BDRCAPIException {
        switch(id.getSubType()) {
        case Identifier.COLLECTION_ID_ITEM:
            return getCollectionForItem(id);
        case Identifier.COLLECTION_ID_WORK_OUTLINE:
            return getCollectionForOutline(id);
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

    public static Collection getCollectionForOutline(final Identifier id) throws BDRCAPIException {
        final WorkInfo wi = WorkInfoService.getWorkInfo(id.getWorkId());
        final ItemInfo ii;
//        if (wi.access != AccessType.OPEN) {
//            throw new BDRCAPIException(403, NO_ACCESS_ERROR_CODE, "you cannot access this collection");
//        }
        logger.info("building outline collection for ID {}", id.getId());
        final Collection collection = new Collection(IIIFPresPrefix_coll+id.getId(), "Collection");
        collection.setAttribution(ManifestService.attribution);
        collection.addLicense("https://creativecommons.org/publicdomain/mark/1.0/");
        collection.addLogo("https://eroux.fr/logo.png");
        if (wi.parts != null) {
            for (WorkInfo.PartInfo pi : wi.parts) {
                final String prefixedPartId = getPrefixedForm(pi.partId);
                final String collectionId = "wio:"+prefixedPartId;
                final Collection subcollection = new Collection(IIIFPresPrefix_coll+collectionId);
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
    
    public static Collection getCollectionForItem(final Identifier id) throws BDRCAPIException {
        final ItemInfo ii = ItemInfoService.getItemInfo(id.getItemId());
//        if (ii.access != AccessType.OPEN) {
//            throw new BDRCAPIException(403, NO_ACCESS_ERROR_CODE, "you cannot access this collection");
//        }
        logger.info("building item collection for ID {}", id.getId());
        final Collection collection = new Collection(IIIFPresPrefix_coll+id.getId(), "Collection");
        collection.setAttribution(ManifestService.attribution);
        collection.addLicense("https://creativecommons.org/publicdomain/mark/1.0/");
        collection.addLogo("https://eroux.fr/logo.png");
        for (ItemInfo.VolumeInfoSmall vi : ii.volumes) {
            final String manifestId = "v:"+vi.getPrefixedUri();
            final String volumeNumberStr = vi.toDisplay();
            final String manifestUrl = vi.iiifManifest == null ? IIIFPresPrefix+manifestId+"/manifest" : vi.iiifManifest; 
            final Manifest manifest = new Manifest(manifestUrl, volumeNumberStr);
            collection.addManifest(manifest);
        }
        return collection;
    }
}
