package io.bdrc.iiif.presentation.models;

import static io.bdrc.iiif.presentation.AppConstants.ADM;
import static io.bdrc.iiif.presentation.AppConstants.BDO;
import static io.bdrc.iiif.presentation.AppConstants.BDR;
import static io.bdrc.iiif.presentation.AppConstants.GENERIC_APP_ERROR_CODE;
import static io.bdrc.iiif.presentation.AppConstants.TMPPREFIX;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.jena.graph.Triple;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.bdrc.iiif.presentation.VolumeInfoService;
import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;

public class VolumeInfo {

    public static final int HAS_TOC = 0;
    public static final int HAS_NO_TOC = 1;
    public static final int TOC_UNKNOWN = 2;
    
    @JsonProperty("access")
    public AccessType access;
    @JsonProperty("license")
    public LicenseType license;
    @JsonProperty("workId")
    public String workId;
    @JsonProperty("itemId")
    public String itemId;
    @JsonProperty("imageList")
    public String imageList;
    @JsonProperty("totalPages")
    public String totalPages;
    @JsonProperty("pagesText")
    public String pagesText="-1";
    @JsonProperty("pagesIntroTbrc")
    public String pagesIntroTbrc="-1";
    @JsonProperty("pagesIntro")
    public String pagesIntro="-1";
    @JsonProperty("imageGroup")
    public String imageGroup = null;
    @JsonProperty("iiifManifest")
    public URI iiifManifest = null;
    @JsonProperty("hasToC")
    public int hasToc;
    @JsonProperty("partInfo")
    public List<PartInfo> partInfo = null;

    private static final Logger logger = LoggerFactory.getLogger(VolumeInfoService.class);

    // result of volumeInfo query
    public VolumeInfo(final QuerySolution sol) {
        logger.debug("creating VolumeInfo for solution {}", sol.toString());
        this.access = AccessType.fromString(sol.getResource("access").getURI());
        this.license = LicenseType.fromString(sol.getResource("license").getURI());
        this.workId = sol.getResource("workId").getURI();
        this.itemId = sol.getResource("itemId").getURI();
        this.hasToc = TOC_UNKNOWN;
        if(sol.contains("?imageList")) {this.imageList = sol.get("?imageList").asLiteral().getString();}
        if(sol.contains("?totalPages")) {this.totalPages = sol.get("?totalPages").asLiteral().getString();}
        if(sol.contains("?pagesText")) {this.pagesText = sol.get("?pagesText").asLiteral().getString();}
        if(sol.contains("?pagesIntroTbrc")) {this.pagesIntroTbrc = sol.get("?pagesIntroTbrc").asLiteral().getString();}
        if(sol.contains("?pagesIntro")) {this.pagesIntro = sol.get("?pagesIntro").asLiteral().getString();}
        if (sol.contains("imageGroup")) {this.imageGroup = sol.getLiteral("imageGroup").getString();}
        if (sol.contains("iiifManifest")) {
            final String manifestURIString = sol.getResource("iiifManifest").getURI();
            try {
                this.iiifManifest = new URI(manifestURIString);
            } catch (URISyntaxException e) {
                logger.error("problem converting sparql result to URI: "+manifestURIString, e);
            }
        }
    }

    public VolumeInfo(final Model m, String volumeId) throws BDRCAPIException {
        logger.debug("creating VolumeInfo for model, volumeId {}", volumeId);
        // the model is supposed to come from the IIIFPres_volumeOutline graph query
        if (volumeId.startsWith("bdr:"))
            volumeId = BDR+volumeId.substring(4);
        final Resource volume = m.getResource(volumeId);
        if (volume == null)
            throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "invalid model: missing volume");
        // checking type (needs to be a bdo:Volume)
        final Triple t = new Triple(volume.asNode(), RDF.type.asNode(), m.getResource(BDO+"VolumeImageAsset").asNode());
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
        
        final Statement imageGroupS = volume.getProperty(m.getProperty(ADM, "legacyImageGroupRID"));
        if (imageGroupS != null) {
            this.imageGroup = imageGroupS.getString();
        }
        
        final Statement volumePagesTotalS = volume.getProperty(m.getProperty(BDO, "volumePagesTotal"));
        if (volumePagesTotalS != null) {
            this.totalPages = volumePagesTotalS.getString();
        }

        final Statement volumePagesTbrcIntroS = volume.getProperty(m.getProperty(BDO, "volumePagesTbrcIntro"));
        if (volumePagesTbrcIntroS != null) {
            this.pagesIntroTbrc = volumePagesTbrcIntroS.getString();
        }

        final Statement volumePagesIntroS = volume.getProperty(m.getProperty(BDO, "volumePagesIntroS"));
        if (volumePagesIntroS != null) {
            this.pagesIntro = volumePagesIntroS.getString();
        }

        final Statement volumePagesTextS = volume.getProperty(m.getProperty(BDO, "volumePagesTextS"));
        if (volumePagesTextS != null) {
            this.pagesText = volumePagesTextS.getString();
        }
        
        final Resource work = item.getPropertyResourceValue(m.getProperty(BDO, "itemImageAssetForWork"));
        if (work == null) {
            throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "invalid model: no associated work");
        }
        this.workId = work.getURI();
        
        final Resource access = work.getPropertyResourceValue(m.getProperty(ADM, "access"));
        if (access != null) {
            this.access = AccessType.fromString(access.getURI());
        } else {
            logger.warn("cannot find model access for {}", workId);
            this.access = AccessType.fromString(BDR+"AccessRestrictedByTbrc");
        }

        final Resource license = work.getPropertyResourceValue(m.getProperty(ADM, "license"));
        if (license != null) {
            this.license = LicenseType.fromString(license.getURI());
        }
        
        this.partInfo = WorkInfo.getParts(m, work);
        if (this.partInfo == null) {
            this.hasToc = HAS_NO_TOC;
        } else {
            this.hasToc = HAS_TOC;
        }
        //this.labels = getLabels(m, volume);
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

    public String getTotalPages() {
        return totalPages;
    }

    public String getPagesText() {
        return pagesText;
    }

    public String getPagesIntroTbrc() {
        return pagesIntroTbrc;
    }

    public String getPagesIntro() {
        return pagesIntro;
    }

    public String getImageGroup() {
        return imageGroup;
    }

    public URI getIiifManifest() {
        return iiifManifest;
    }

    public VolumeInfo() { }

    @Override
    public String toString() {
        return "VolumeInfo [access=" + access + ", license=" + license + ", workId=" + workId + ", itemId=" + itemId
                + ", imageList=" + imageList + ", imageGroup=" + imageGroup + ", iiifManifest=" + iiifManifest + "]";
    }
}
