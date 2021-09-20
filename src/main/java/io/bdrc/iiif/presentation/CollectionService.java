package io.bdrc.iiif.presentation;

import static io.bdrc.iiif.presentation.AppConstants.BDR_len;
import static io.bdrc.iiif.presentation.AppConstants.GENERIC_APP_ERROR_CODE;
import static io.bdrc.iiif.presentation.AppConstants.IIIFPresPrefix;
import static io.bdrc.iiif.presentation.AppConstants.IIIFPresPrefix_coll;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.digitalcollections.iiif.model.PropertyValue;
import de.digitalcollections.iiif.model.sharedcanvas.Collection;
import de.digitalcollections.iiif.model.sharedcanvas.Manifest;
import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.resmodels.ImageInstanceInfo;
import io.bdrc.iiif.presentation.resmodels.LangString;
import io.bdrc.iiif.presentation.resmodels.Location;
import io.bdrc.iiif.presentation.resmodels.PartInfo;
import io.bdrc.iiif.presentation.resmodels.InstanceInfo;
import io.bdrc.iiif.presentation.resmodels.ImageInstanceInfo.VolumeInfoSmall;
import io.bdrc.iiif.presentation.resservices.ImageInstanceInfoService;
import io.bdrc.iiif.presentation.resservices.InstanceInfoService;

public class CollectionService {

    private static final Logger logger = LoggerFactory.getLogger(CollectionService.class);
    // public static final List<ViewingHint> VIEW_HINTS=Arrays.asList(new
    // ViewingHint[] { ViewingHint.MULTI_PART});
    public static final String VIEWING_HINTS = "multi-part";

    public static String getQname(final String uri) {
        return "bdr:" + uri.substring(BDR_len);
    }

    public static Collection getCollectionForIdentifier(final Identifier id, boolean continuous) throws BDRCAPIException {
        InstanceInfo wi = null;
        if (id.getInstanceId() != null) {
            try {
                wi = InstanceInfoService.Instance.getAsync(id.getInstanceId()).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new BDRCAPIException(404, AppConstants.GENERIC_IDENTIFIER_ERROR, e);
            }
        }
        switch (id.getSubType()) {
        case Identifier.COLLECTION_ID_ITEM:
        case Identifier.COLLECTION_ID_ITEM_VOLUME_OUTLINE:
        case Identifier.COLLECTION_ID_WORK_IN_ITEM:
            return getCollectionForItem(getCommonCollection(id), id, wi, continuous);
        case Identifier.COLLECTION_ID_WORK_OUTLINE:
            return getCollectionForOutline(getCommonCollection(id), id, wi, continuous);
        case Identifier.COLLECTION_ID_WORK_OUTLINE_VOLUME:
            return getCollectionForOutlinePerVolume(getCommonCollection(id), id, wi, continuous);
        default:
            throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "you cannot access this type of manifest yet");
        }
    }

    // not used anymore but quite convenient
    public static void addManifestsForLocation(final Collection c, final InstanceInfo wi, final ImageInstanceInfo ii, final boolean continuous) {
        if (wi.locations == null)
            return;
        boolean needsVolumeIndication = false;
        Integer lastVolume = null;
        for (final Location loc : wi.locations) {
            if (loc.evolnum - loc.bvolnum > 2) {
                needsVolumeIndication = true;
                break;
            }
            if (lastVolume != null && lastVolume != loc.bvolnum) {
                needsVolumeIndication = true;
                break;
            }
            lastVolume = loc.bvolnum;
        }
        for (final Location loc: wi.locations) {
            for (int i = loc.bvolnum; i <= loc.evolnum; i++) {
                VolumeInfoSmall vi = ii.getVolumeNumber(i);
                if (vi == null)
                    continue;
                final int volumebPage = (i == loc.bvolnum.intValue()) ? loc.bpagenum : 0;
                final int volumeePage = (i == loc.evolnum.intValue()) ? loc.epagenum : -1;
                final StringBuilder sb = new StringBuilder();
                sb.append(IIIFPresPrefix + "vo:" + vi.getPrefixedUri());
                if (volumebPage != 0 || volumeePage != -1) {
                    sb.append("::");
                    if (volumebPage != 0)
                        sb.append(volumebPage);
                    sb.append("-");
                    if (volumeePage != -1)
                        sb.append(volumeePage);
                }
                sb.append("/manifest");
                if (continuous) {
                    sb.append("?continuous=true");
                }
                final Manifest m = new Manifest(sb.toString());
                m.setLabel(ManifestService.getPVforLS(wi.labels, needsVolumeIndication ? i : null));
                c.addManifest(m);
            }
        }
    }
    
    public static void addManifestsForWorkInVolumes(final Collection c, final InstanceInfo wi, final ImageInstanceInfo ii, final boolean continuous, final String workId) {
        if (!wi.hasLocation)
            return;
        List<Integer> vols = new ArrayList<>();
        for (final Location loc : wi.locations) {
            for (int i = loc.bvolnum; i <= loc.evolnum; i++) {
                if (!vols.contains(i)) {
                    vols.add(i);
                }
            }
        }
        final boolean needsVolumeIndication = vols.size() > 1;
        for (final Integer i : vols) {
            VolumeInfoSmall vi = ii.getVolumeNumber(i);
            if (vi == null)
                continue;
            final StringBuilder sb = new StringBuilder();
            sb.append(IIIFPresPrefix + "wvo:" + workId + "::" + vi.getPrefixedUri() + "/manifest");
            if (continuous) {
                sb.append("?continuous=true");
            }
            final Manifest m = new Manifest(sb.toString());
            m.setLabel(ManifestService.getPVforLS(wi.labels, needsVolumeIndication ? i : null));
            c.addManifest(m);
        }
    }

    public static void addManifestsForLocation(final Collection c, List<Location> ll, final ImageInstanceInfo ii, final boolean continuous, final String workQname, final boolean withOutline) {
        if (ll == null)
            return;
        List<Integer> vols = new ArrayList<>();
        for (final Location loc : ll) {
            for (int i = loc.bvolnum; i <= loc.evolnum; i++) {
                if (!vols.contains(i)) {
                    vols.add(i);
                }
            }
        }
        for (final Integer i : vols) {
            VolumeInfoSmall vi = ii.getVolumeNumber(i);
            if (vi == null)
                continue;
            final StringBuilder sb = new StringBuilder();
            final String prefix = withOutline ? "wvo:" : "wv:";
            sb.append(IIIFPresPrefix + prefix + workQname + "::" + vi.getPrefixedUri() + "/manifest");
            if (continuous) {
                sb.append("?continuous=true");
            }
            final Manifest m = new Manifest(sb.toString());
            m.setLabel(ManifestService.getVolNumPV(i));
            c.addManifest(m);
        }
    }
    
    public static PropertyValue getLabels(String workId, List<LangString> labels) {
        final PropertyValue label = new PropertyValue();
        if (labels == null || labels.isEmpty()) {
            label.addValue(workId);
            return label;
        }
        for (LangString ls : labels) {
            if (ls.language != null)
                label.addValue(ManifestService.getLocaleFor(ls.language), ls.value);
            else
                label.addValue(ls.value);
        }
        return label;
    }

    public static Collection getCommonCollection(final Identifier id) {
        final Collection collection = new Collection(IIIFPresPrefix_coll + id.getId(), "Collection");
        collection.setAttribution(ManifestService.attribution);
        collection.setViewingHints(VIEWING_HINTS);
        // TODO: use the actual license
        collection.addLicense("https://creativecommons.org/publicdomain/mark/1.0/");
        collection.addLogo("https://s3.amazonaws.com/bdrcwebassets/prod/iiif-logo.png");
        return collection;
    }

    public static Collection getCollectionForOutlinePerVolume(final Collection collection, final Identifier id, final InstanceInfo iInf, final boolean continuous) throws BDRCAPIException {
        final ImageInstanceInfo iiInf;
        logger.info("building outline in collection per volume for ID {}", id.getId());
        collection.setLabel(getLabels(id.getInstanceId(), iInf.labels));

        if (id.getImageInstanceId() != null) {
            try {
                iiInf = ImageInstanceInfoService.Instance.getAsync(id.getImageInstanceId()).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, e);
            }
        } else if (iInf.imageInstanceQname != null) {
            try {
                iiInf = ImageInstanceInfoService.Instance.getAsync(iInf.imageInstanceQname).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, e);
            }
        } else {
            // TODO: exception of wi.parts == null ? currently an empty collection is
            // returned
            return collection;
        }
        VolumeInfoSmall vis = iiInf.getImageGroup(id.imageGroupId);
        if (vis == null || vis.volumeNumber == null)
            return collection;
        boolean hasSeenpartinvolume = false;
        if (iInf.parts != null) {
            for (final PartInfo pi : iInf.parts) {
                if (pi.isInVolumeR(vis.volumeNumber)) {
                    hasSeenpartinvolume = true;
                    final String collectionId = "wio:" + pi.partQname;
                    final Collection subcollection = new Collection(IIIFPresPrefix_coll + collectionId);
                    final PropertyValue labels = ManifestService.getPropForLabels(pi.labels);
                    subcollection.setLabel(labels);
                    collection.addCollection(subcollection);
                } else if (hasSeenpartinvolume) {
                    // we assume that once we have seen some parts in a volume then some parts not in the volume,
                    // no further part will be in the volume
                    break;
                }
            }
        }

        final String volPrefix = "vo:";
        boolean needsVolumeIndication = iiInf.volumes.size() > 1;
        
        final String manifestId = volPrefix + vis.getPrefixedUri();
        String manifestUrl;
        if (vis.iiifManifestUri != null) {
            manifestUrl = vis.iiifManifestUri;
        } else {
            manifestUrl = IIIFPresPrefix + manifestId + "/manifest";
            if (continuous) {
                manifestUrl += "?continuous=true";
            }
        }
        final Manifest manifest = new Manifest(manifestUrl);
        manifest.setLabel(ManifestService.getPVforLS(iInf.labels, needsVolumeIndication ? vis.volumeNumber : null));
        collection.addManifest(manifest);
        return collection;
    }
    
    public static Collection getCollectionForOutline(final Collection collection, final Identifier id, final InstanceInfo iInf, final boolean continuous) throws BDRCAPIException {
        final ImageInstanceInfo iiInf;
        logger.info("building outline collection for ID {}", id.getId());
        collection.setLabel(getLabels(id.getInstanceId(), iInf.labels));
        if (iInf.parts != null) {
            for (final PartInfo pi : iInf.parts) {
                final String collectionId = "wio:" + pi.partQname;
                final Collection subcollection = new Collection(IIIFPresPrefix_coll + collectionId);
                final PropertyValue labels = ManifestService.getPropForLabels(pi.labels);
                subcollection.setLabel(labels);
                collection.addCollection(subcollection);
            }
        }
        if (id.getImageInstanceId() != null) {
            try {
                iiInf = ImageInstanceInfoService.Instance.getAsync(id.getImageInstanceId()).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, e);
            }
        } else if (iInf.imageInstanceQname != null) {
            try {
                iiInf = ImageInstanceInfoService.Instance.getAsync(iInf.imageInstanceQname).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, e);
            }
        } else {
            // TODO: exception of wi.parts == null ? currently an empty collection is
            // returned
            return collection;
        }
        if (iInf.hasLocation) {
            // addManifestsForLocation(collection, wi, ii, continuous);
            addManifestsForWorkInVolumes(collection, iInf, iiInf, continuous, id.getInstanceId());
         // special case for the Taisho where we have many items
        } else if (iInf.isRoot || id.getInstanceId().equals(getQname(iiInf.instanceUri))) {
            final String volPrefix = "vo:";
            boolean needsVolumeIndication = iiInf.volumes.size() > 1;
            for (ImageInstanceInfo.VolumeInfoSmall vi : iiInf.volumes) {
                final String manifestId = volPrefix + vi.getPrefixedUri();
                String manifestUrl;
                if (vi.iiifManifestUri != null) {
                    manifestUrl = vi.iiifManifestUri;
                } else {
                    manifestUrl = IIIFPresPrefix + manifestId + "/manifest";
                    if (continuous) {
                        manifestUrl += "?continuous=true";
                    }
                }
                final Manifest manifest = new Manifest(manifestUrl);
                manifest.setLabel(ManifestService.getPVforLS(iInf.labels, needsVolumeIndication ? vi.volumeNumber : null));
                collection.addManifest(manifest);
            }
        }
        return collection;
    }

    public static Collection getCollectionForItem(final Collection collection, final Identifier id, final InstanceInfo wi, final boolean continuous) throws BDRCAPIException {
        final String itemId = (id.getImageInstanceId() == null) ? wi.imageInstanceQname : id.getImageInstanceId();
        final ImageInstanceInfo ii;
        try {
            ii = ImageInstanceInfoService.Instance.getAsync(itemId).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, e);
        }
        logger.info("building item collection for ID {}", id.getId());
        collection.addLabel(id.getImageInstanceId());
        if (ii.locations != null) {
            // case of an image instance with location
            addManifestsForLocation(collection, ii.locations, ii, continuous, itemId, id.getSubType() == Identifier.COLLECTION_ID_ITEM_VOLUME_OUTLINE);
        } else {
            // more general case
            final String volPrefix = id.getSubType() == Identifier.COLLECTION_ID_ITEM_VOLUME_OUTLINE ? "vo:" : "v:";
            for (ImageInstanceInfo.VolumeInfoSmall vi : ii.volumes) {
                final String manifestId = volPrefix + vi.getPrefixedUri();
                String manifestUrl;
                if (vi.iiifManifestUri != null) {
                    manifestUrl = vi.iiifManifestUri;
                } else {
                    manifestUrl = IIIFPresPrefix + manifestId + "/manifest";
                    if (continuous) {
                        manifestUrl += "?continuous=true";
                    }
                }
                final Manifest manifest = new Manifest(manifestUrl);
                manifest.setLabel(vi.getLabel());
                collection.addManifest(manifest);
            }
        }
        return collection;
    }
}
