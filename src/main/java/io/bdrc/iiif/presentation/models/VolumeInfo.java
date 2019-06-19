package io.bdrc.iiif.presentation.models;

import static io.bdrc.iiif.presentation.AppConstants.BDO;
import static io.bdrc.iiif.presentation.AppConstants.BDR;
import static io.bdrc.iiif.presentation.AppConstants.GENERIC_APP_ERROR_CODE;
import static io.bdrc.iiif.presentation.AppConstants.TMPPREFIX;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;

import org.apache.jena.graph.Triple;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.bdrc.iiif.presentation.VolumeInfoService;
import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;

public class VolumeInfo {

    @JsonProperty("access")
    public AccessType access;
    @JsonProperty("restrictedInChina")
    public Boolean restrictedInChina = false;
    @JsonProperty("license")
    public LicenseType license;
    @JsonProperty("status")
    public String statusUri;
    @JsonProperty("workId")
    public String workId;
    @JsonProperty("itemId")
    public String itemId;
    @JsonProperty("imageList")
    public String imageList;
    @JsonProperty("totalPages")
    public Integer totalPages;
    @JsonProperty("pagesIntroTbrc")
    public Integer pagesIntroTbrc = 0;
    @JsonProperty("volumeNumber")
    public Integer volumeNumber = 1;
    @JsonProperty("imageGroup")
    public String imageGroup = null;
    @JsonProperty("iiifManifest")
    public URI iiifManifest = null;
    @JsonProperty("partInfo")
    public List<PartInfo> partInfo = null;

    private static final Logger logger = LoggerFactory.getLogger(VolumeInfoService.class);

    // result of volumeInfo query
    public VolumeInfo(final QuerySolution sol) {
        logger.debug("creating VolumeInfo for solution {}", sol.toString());
        this.access = AccessType.fromString(sol.getResource("access").getURI());
        this.statusUri = sol.getResource("status").getURI();
        this.license = LicenseType.fromString(sol.getResource("license").getURI());
        this.workId = sol.getResource("workId").getURI();
        this.itemId = sol.getResource("itemId").getURI();
        if (sol.contains("?ric")) {
            this.restrictedInChina = sol.get("?ric").asLiteral().getBoolean();
        }
        if (sol.contains("?imageList")) {
            this.imageList = sol.get("?imageList").asLiteral().getString();
        }
        if (sol.contains("?volumeNumber")) {
            this.volumeNumber = sol.get("?volumeNumber").asLiteral().getInt();
        }
        if (sol.contains("?totalPages")) {
            this.totalPages = sol.get("?totalPages").asLiteral().getInt();
        }
        if (sol.contains("?pagesIntroTbrc")) {
            this.pagesIntroTbrc = sol.get("?pagesIntroTbrc").asLiteral().getInt();
        }
        if (sol.contains("imageGroup")) {
            this.imageGroup = sol.getLiteral("imageGroup").getString();
        }
        if (sol.contains("iiifManifest")) {
            final String manifestURIString = sol.getResource("iiifManifest").getURI();
            try {
                this.iiifManifest = new URI(manifestURIString);
            } catch (URISyntaxException e) {
                logger.error("problem converting sparql result to URI: " + manifestURIString, e);
            }
        }
    }

    private void fromModel(final Model m, String volumeId) throws BDRCAPIException {
        logger.debug("creating VolumeInfo for model, volumeId {}", volumeId);
        // the model is supposed to come from the IIIFPres_volumeOutline graph query
        if (volumeId.startsWith("bdr:"))
            volumeId = BDR + volumeId.substring(4);
        final Resource volume = m.getResource(volumeId);
        if (volume == null)
            throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "invalid model: missing volume");
        // checking type (needs to be a bdo:Volume)
        final Triple t = new Triple(volume.asNode(), RDF.type.asNode(), m.getResource(BDO + "VolumeImageAsset").asNode());
        final ExtendedIterator<Triple> ext = m.getGraph().find(t);
        if (!ext.hasNext()) {
            throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "invalid model: not a volume");
        }
        final Resource item = volume.getPropertyResourceValue(m.getProperty(BDO, "volumeOf"));
        if (item == null) {
            throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "invalid model: no associated item");
        }
        this.itemId = item.getURI();

        final Statement imageListS = volume.getProperty(m.getProperty(BDO, "imageList"));
        if (imageListS != null) {
            this.imageList = imageListS.getString();
        }

        final Statement imageGroupS = volume.getProperty(m.getProperty(TMPPREFIX, "legacyImageGroupRID"));
        if (imageGroupS != null) {
            this.imageGroup = imageGroupS.getString();
        }

        final Statement volumePagesTotalS = volume.getProperty(m.getProperty(BDO, "volumePagesTotal"));
        if (volumePagesTotalS != null) {
            this.totalPages = volumePagesTotalS.getInt();
        }

        final Statement iiifManifestS = volume.getProperty(m.getProperty(BDO, "hasIIIFManifest"));
        if (iiifManifestS != null) {
            try {
                this.iiifManifest = new URI(iiifManifestS.getResource().getURI());
            } catch (URISyntaxException e) {
                logger.error("problem converting sparql graph result to URI: " + iiifManifestS.getResource().getURI(), e);
            }
        }

        final Statement volumeNumberS = volume.getProperty(m.getProperty(BDO, "volumeNumber"));
        if (volumeNumberS != null) {
            this.volumeNumber = volumeNumberS.getInt();
        }

        final Statement volumePagesTbrcIntroS = volume.getProperty(m.getProperty(BDO, "volumePagesTbrcIntro"));
        if (volumePagesTbrcIntroS != null) {
            this.pagesIntroTbrc = volumePagesTbrcIntroS.getInt();
        }

        final Resource work = item.getPropertyResourceValue(m.getProperty(BDO, "itemForWork"));
        if (work == null) {
            throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "invalid model: no associated work");
        }
        this.workId = work.getURI();

        final Resource access = volume.getPropertyResourceValue(m.getProperty(TMPPREFIX, "rootAccess"));
        if (access != null) {
            this.access = AccessType.fromString(access.getURI());
        } else {
            logger.warn("cannot find model access for {}", workId);
            this.access = AccessType.fromString(BDR + "AccessRestrictedByTbrc");
        }
        
        final Resource status = volume.getPropertyResourceValue(m.getProperty(TMPPREFIX, "rootStatus"));
        if (access != null) {
            this.statusUri = status.getURI();
        } else {
            logger.warn("cannot find model status for {}", workId);
            this.statusUri = null;
        }
        
        final Resource license = work.getPropertyResourceValue(m.getProperty(TMPPREFIX, "rootLicense"));
        if (license != null) {
            this.license = LicenseType.fromString(license.getURI());
        }

        final Statement restrictedInChinaS = volume.getProperty(m.getProperty(TMPPREFIX, "rootRestrictedInChina"));
        if (restrictedInChinaS == null) {
            this.restrictedInChina = true;
        } else {
            this.restrictedInChina = restrictedInChinaS.getBoolean();
        }

        this.partInfo = WorkInfo.getParts(m, work);
        // this.labels = getLabels(m, volume);
    }

    public VolumeInfo(final Model m, final String volumeId) throws BDRCAPIException {
        fromModel(m, volumeId);
    }

    public VolumeInfo(final Model m, String workId, String volumeId) throws BDRCAPIException {
        // result of IIIFPres_workGraph_noItem
        if (volumeId != null) {
            fromModel(m, volumeId);
            return;
        }
        if (workId.startsWith("bdr:"))
            workId = BDR + workId.substring(4);
        final Resource work = m.getResource(workId);
        final Resource firstVolume = work.getPropertyResourceValue(m.getProperty(TMPPREFIX, "firstVolume"));
        if (firstVolume != null) {
            volumeId = firstVolume.getURI();
            fromModel(m, volumeId);
        } else {
            throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "cannot get volumeId from work model");
        }
    }

    public Iterator<String> getImageListIterator(int beginIdx, int endIdx) {
        return new ImageListIterator(imageList, beginIdx, endIdx);
    }

    public AccessType getAccess() {
        return access;
    }

    public LicenseType getLicense() {
        return license;
    }

    public String getWorkId() {
        return workId;
    }

    public String getItemId() {
        return itemId;
    }

    public String getImageList() {
        return imageList;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public Integer getPagesIntroTbrc() {
        return pagesIntroTbrc;
    }

    public String getImageGroup() {
        return imageGroup;
    }

    public URI getIiifManifest() {
        return iiifManifest;
    }

    public VolumeInfo() {
    }

    @Override
    public String toString() {
        return "VolumeInfo [access=" + access + ", license=" + license + ", workId=" + workId + ", itemId=" + itemId + ", imageList=" + imageList + ", imageGroup=" + imageGroup + ", iiifManifest=" + iiifManifest + "]";
    }
}
