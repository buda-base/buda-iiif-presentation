package io.bdrc.iiif.presentation.models;

import java.net.URI;
import java.net.URISyntaxException;

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
        if (sol.contains("imageGroup")) {
            this.imageGroup = sol.getLiteral("imageGroup").getString();
        }
        if (sol.contains("iiifManifest")) {
            final String manifestURIString = sol.getResource("iiifManifest").getURI(); 
            try {
                this.iiifManifest = new URI(manifestURIString);
            } catch (URISyntaxException e) {
                logger.error("problem converting sparql result to URI: "+manifestURIString, e);
            }
        }
    }
    
    public VolumeInfo() { }
}
