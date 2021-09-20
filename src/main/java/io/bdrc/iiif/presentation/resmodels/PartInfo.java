package io.bdrc.iiif.presentation.resmodels;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PartInfo implements Comparable<PartInfo> {
    @JsonProperty("partIndex")
    public Integer partIndex = null;
    @JsonProperty("partQname")
    public String partQname = null;
    @JsonProperty("labels")
    public List<LangString> labels = null;
    @JsonProperty("parts")
    public List<PartInfo> parts = null;
    @JsonProperty("locations")
    public List<Location> locations = null;
    @JsonProperty("linkToQname")
    public String linkToQname = null;
    @JsonProperty("linkToTypeLname")
    public String linkToTypeLname = null;
    
    public PartInfo(final String partQname, final Integer partIndex) {
        this.partQname = partQname;
        this.partIndex = partIndex;
    }
    
    public PartInfo() {}
    
    @Override
    public int compareTo(PartInfo compared) {
        if (this.partIndex == null || compared.partIndex == null)
            return 0;
        return this.partIndex - compared.partIndex;
    }
    
    @Override
    public String toString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "toString objectmapper exception, this shouldn't happen";
        }
    }
    
    public boolean isInVolumeR(int volNum) {
        if (this.locations != null) {
            for (final Location loc : this.locations) {
                if (loc.bvolnum <= volNum && loc.evolnum >= volNum) {
                    return true;
                }
            }
            return false;
        }
        if (this.parts == null)
            return false;
        for (final PartInfo child : this.parts) {
            if (child.isInVolumeR(volNum)) {
                return true;
            }
        }
        return false;
    }
}