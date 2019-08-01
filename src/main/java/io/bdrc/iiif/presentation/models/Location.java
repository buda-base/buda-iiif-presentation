package io.bdrc.iiif.presentation.models;

import static io.bdrc.iiif.presentation.AppConstants.BDO;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Location {
    @JsonProperty("bvolnum")
    public Integer bvolnum = null;
    @JsonProperty("evolnum")
    public Integer evolnum = null;
    @JsonProperty("bpagenum")
    public Integer bpagenum = null;
    @JsonProperty("epagenum") // by convention, epagenum is -1 for the last page
    public Integer epagenum = null;
    
    
    public Location(final Model m, final Resource location) {
        final Property locationVolumeP = m.getProperty(BDO, "workLocationVolume");
        if (!location.hasProperty(locationVolumeP))
            this.bvolnum = 1; // probable reasonable default...
        else 
            this.bvolnum = location.getProperty(locationVolumeP).getInt();
        final Property locationEndVolumeP = m.getProperty(BDO, "workLocationEndVolume");
        // a stupid temporary mistake in the data
        final Property locationEndVolumeTmpP = m.getProperty(BDO, "workLocationVolumeEnd");
        if (location.hasProperty(locationEndVolumeP)) {
            this.evolnum = location.getProperty(locationEndVolumeP).getInt();
        } else if (location.hasProperty(locationEndVolumeTmpP)) {
            this.evolnum = location.getProperty(locationEndVolumeTmpP).getInt();
        } else {
            this.evolnum = this.bvolnum;
        }
        final Property locationPageP = m.getProperty(BDO, "workLocationPage");
        if (location.hasProperty(locationPageP))
            this.bpagenum = location.getProperty(locationPageP).getInt();
        else
            this.bpagenum = 0;
        final Property locationEndPageP = m.getProperty(BDO, "workLocationEndPage");
        if (location.hasProperty(locationEndPageP))
            this.epagenum = location.getProperty(locationEndPageP).getInt();
        else
            this.epagenum = -1;
    }
    
    @Override
    public String toString() {
        return "Location [bvolnum=" + bvolnum + ", evolnum=" + evolnum
                + ", bpagenum=" + bpagenum + ", epagenum=" + epagenum + "]";
    }
}
