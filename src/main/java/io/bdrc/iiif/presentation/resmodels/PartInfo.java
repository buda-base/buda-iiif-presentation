package io.bdrc.iiif.presentation.resmodels;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    @JsonProperty("partType")
    public PartType partType = PartType.OTHER;
    
    public static enum PartType {
        VOLUME, SECTION, OTHER 
    }
    
    private static final Logger logger = LoggerFactory.getLogger(PartInfo.class);
    
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
    
    public boolean needsPerVolumeSubsections() {
        if (this.partType != PartType.SECTION)
            return false;
        if (this.parts == null)
            return false;
        for (PartInfo pi : this.parts) {
            if (pi.partType != PartType.OTHER)
                return false;
        }
        return true;
    }
    
    // could be improved with some recursivity
    public Integer[] getFirstAndLastVolnum() {
        Integer[] res = new Integer[2];
        if (this.locations != null && !this.locations.isEmpty()) {
            // locations are in order:
            Location l = this.locations.get(0);
            res[0] = l.bvolnum;
            l = this.locations.get(this.locations.size()-1);
            res[1] = l.evolnum;
            return res;
        }
        // parts are in order too:
        if (this.parts != null && !this.parts.isEmpty()) {
            PartInfo pi = this.parts.get(0);
            if (pi.locations == null || pi.locations.isEmpty()) return null;
            Location l = pi.locations.get(0);
            res[0] = l.bvolnum;
            pi = this.parts.get(this.parts.size()-1);
            if (pi.locations == null || pi.locations.isEmpty()) return null;
            l = pi.locations.get(pi.locations.size()-1);
            res[1] = l.evolnum;
            return res;
        }
        return null;
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