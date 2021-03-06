package io.bdrc.iiif.presentation.resmodels;

import static io.bdrc.iiif.presentation.AppConstants.BDO;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Location implements Comparable<Location> {
    @JsonProperty("bvolnum")
    public Integer bvolnum = null;
    @JsonProperty("evolnum")
    public Integer evolnum = null;
    @JsonProperty("bpagenum") // stays null if no begin page indicated
    public Integer bpagenum = null;
    @JsonProperty("epagenum") // by convention, epagenum is -1 for the last page
    public Integer epagenum = null;
    @JsonProperty("instanceUri")
    public String instanceUri = null;
    
    
    public Location(final Model m, final Resource location) {
        final Property locationVolumeP = m.getProperty(BDO, "contentLocationVolume");
        if (!location.hasProperty(locationVolumeP))
            this.bvolnum = 1; // probable a reasonable default...
        else 
            this.bvolnum = location.getProperty(locationVolumeP).getInt();
        final Property locationEndVolumeP = m.getProperty(BDO, "contentLocationEndVolume");
        // a stupid temporary mistake in the data
        final Property locationEndVolumeTmpP = m.getProperty(BDO, "contentLocationVolumeEnd");
        if (location.hasProperty(locationEndVolumeP)) {
            this.evolnum = location.getProperty(locationEndVolumeP).getInt();
        } else if (location.hasProperty(locationEndVolumeTmpP)) {
            this.evolnum = location.getProperty(locationEndVolumeTmpP).getInt();
        } else {
            this.evolnum = this.bvolnum;
        }
        final Property locationPageP = m.getProperty(BDO, "contentLocationPage");
        if (location.hasProperty(locationPageP))
            this.bpagenum = location.getProperty(locationPageP).getInt();
        final Property locationEndPageP = m.getProperty(BDO, "contentLocationEndPage");
        if (location.hasProperty(locationEndPageP))
            this.epagenum = location.getProperty(locationEndPageP).getInt();
        else
            this.epagenum = -1;
        final Property workLocationWorkP = m.getProperty(BDO, "contentLocationInstance");
        if (location.hasProperty(workLocationWorkP))
            this.instanceUri = location.getPropertyResourceValue(workLocationWorkP).getURI();
    }
    
    @Override
    public String toString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "toString objectmapper exception, this shouldn't happen";
        }
    }

    @Override
    public int compareTo(Location compared) {
        int voldiff = this.bvolnum - compared.bvolnum;
        if (voldiff != 0) return voldiff;
        if (this.bpagenum != null && compared.bpagenum != null) {
            return this.bpagenum - compared.bpagenum;
        }
        return 0;
    }
}
