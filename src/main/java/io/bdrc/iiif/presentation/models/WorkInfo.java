package io.bdrc.iiif.presentation.models;

import org.apache.jena.rdf.model.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class WorkInfo {
   
    @JsonProperty("access")
    public AccessType access;
    @JsonProperty("license")
    public LicenseType license;
    @JsonIgnore
    public Model model;
}
