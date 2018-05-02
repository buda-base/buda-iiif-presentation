package io.bdrc.iiif.presentation.models;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ItemInfo {
    
    @JsonProperty("workId")
    public String workId;
    @JsonProperty("access")
    public AccessType access;
    @JsonProperty("license")
    public LicenseType license;
    @JsonProperty("nbVolumes")
    public int nbVolumes;
    @JsonIgnore
    public Model model;
    
    public ItemInfo(QuerySolution sol) {
        this.access = AccessType.fromString(sol.get("access").asResource().getURI());
        this.license = LicenseType.fromString(sol.get("license").asResource().getURI());
        this.workId = sol.get("work").asResource().getURI();
        this.nbVolumes = sol.get("nbVolumes").asLiteral().getInt();
    }
    
    public ItemInfo() {
        
    }
}
