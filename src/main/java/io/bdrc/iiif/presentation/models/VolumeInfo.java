package io.bdrc.iiif.presentation.models;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;

import org.apache.jena.query.QuerySolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.bdrc.iiif.presentation.VolumeInfoService;

public class VolumeInfo {

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
    public String pagesIntroTbrc="-1";;
    @JsonProperty("pagesIntro")
    public String pagesIntro="-1";;
    @JsonProperty("imageGroup")
    public String imageGroup = null;
    @JsonProperty("iiifManifest")
    public URI iiifManifest = null;

    private static final Logger logger = LoggerFactory.getLogger(VolumeInfoService.class);

    public VolumeInfo(QuerySolution sol) {
        logger.debug("creating VolumeInfo for solution {}", sol.toString());
        this.access = AccessType.fromString(sol.getResource("access").getURI());
        this.license = LicenseType.fromString(sol.getResource("license").getURI());
        this.workId = sol.getResource("workId").getURI();
        this.itemId = sol.getResource("itemId").getURI();
        this.imageList = sol.get("?imageList").asLiteral().getString();
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
