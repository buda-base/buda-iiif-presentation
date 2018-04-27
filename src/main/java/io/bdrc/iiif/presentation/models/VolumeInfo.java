package io.bdrc.iiif.presentation.models;

import org.apache.jena.query.QuerySolution;

import com.fasterxml.jackson.annotation.JsonProperty;

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
    public String imageGroup;
    
    public VolumeInfo(QuerySolution sol) {
        this.access = AccessType.fromString(sol.get("access").asResource().getURI());
        this.license = LicenseType.fromString(sol.get("license").asResource().getURI());
        this.workId = sol.get("work").asResource().getURI();
        this.itemId = sol.get("item").asResource().getURI();
        this.imageGroup = sol.get("imageGroup").asLiteral().getString();
    }
    
    public VolumeInfo() {
        
    }
}
