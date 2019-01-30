package io.bdrc.iiif.presentation.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PartInfo implements Comparable<PartInfo> {
    @JsonProperty("partIndex")
    public final Integer partIndex;
    @JsonProperty("partId")
    public final String partId;
    @JsonProperty("labels")
    List<LangString> labels = null;
    @JsonProperty("subparts")
    List<PartInfo> subparts = null;
    @JsonProperty("location")
    public Location location = null;
    
    public PartInfo(final String partId, final Integer partIndex) {
        this.partId = partId;
        this.partIndex = partIndex;
    }
    
    @Override
    public int compareTo(PartInfo compared) {
        if (this.partIndex == null || compared.partIndex == null)
            return 0;
        return this.partIndex - compared.partIndex;
    }
}