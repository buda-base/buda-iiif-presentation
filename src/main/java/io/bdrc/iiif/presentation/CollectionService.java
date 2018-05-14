package io.bdrc.iiif.presentation;

import de.digitalcollections.iiif.model.sharedcanvas.Collection;
import de.digitalcollections.iiif.model.sharedcanvas.Manifest;
import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.models.AccessType;
import io.bdrc.iiif.presentation.models.Identifier;
import io.bdrc.iiif.presentation.models.ItemInfo;

import static io.bdrc.iiif.presentation.AppConstants.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CollectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(ManifestService.class);
    
    public static String getPrefixedForm(String id) {
        return "bdr:"+id.substring(BDR_len);
    }
    
    public static Collection getCollectionForIdentifier(final Identifier id) throws BDRCAPIException {
        if (id.getType() != Identifier.COLLECTION_ID || id.getSubType() != Identifier.COLLECTION_ID_ITEM) {
            throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "you cannot access this type of manifest yet");
        }
        final ItemInfo ii = ItemInfoService.getItemInfo(id.getItemId());
        if (ii.access != AccessType.OPEN) {
            throw new BDRCAPIException(403, NO_ACCESS_ERROR_CODE, "you cannot access this collection");
        }
        logger.info("building collection for ID {}", id.getId());
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
