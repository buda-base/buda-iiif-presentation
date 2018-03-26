package io.bdrc.iiif.presentation.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VolumeBasicInfo {
    @JsonProperty("access")
    public AccessType access;
    @JsonProperty("license")
    public LicenseType license;
    @JsonProperty("workId")
    public String workId;
    @JsonProperty("itemId")
    public String itemId;
}
