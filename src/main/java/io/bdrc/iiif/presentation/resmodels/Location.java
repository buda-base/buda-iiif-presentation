package io.bdrc.iiif.presentation.resmodels;

import static io.bdrc.iiif.presentation.AppConstants.BDO;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    
    private static final Logger logger = LoggerFactory.getLogger(Location.class);
    
    final private static Property locationVolumeP = ResourceFactory.createProperty(BDO, "contentLocationVolume");
    final private static Property locationEndVolumeP = ResourceFactory.createProperty(BDO, "contentLocationEndVolume");
    final private static Property locationPageP = ResourceFactory.createProperty(BDO, "contentLocationPage");
    final private static Property locationEndPageP = ResourceFactory.createProperty(BDO, "contentLocationEndPage");
    final private static Property workLocationWorkP = ResourceFactory.createProperty(BDO, "contentLocationInstance");
    
    public Location(final Model m, final Resource location) {
        if (!location.hasProperty(locationVolumeP)) {
            this.bvolnum = 1; // probably a reasonable default...
            //logger.info("no location beginning volume for "+location.getLocalName());
        } else { 
            this.bvolnum = location.getProperty(locationVolumeP).getInt();
        }
        if (location.hasProperty(locationEndVolumeP)) {
            this.evolnum = location.getProperty(locationEndVolumeP).getInt();
        } else {
            this.evolnum = this.bvolnum;
        }
        if (location.hasProperty(locationPageP)) {
            try {
                this.bpagenum = location.getProperty(locationPageP).getInt();
                if (this.bpagenum < 1)
                    this.bpagenum = null;
            } catch (Exception e) { }
        }
        if (location.hasProperty(locationEndPageP)) {
            try {
                this.epagenum = location.getProperty(locationEndPageP).getInt();
                if (this.epagenum < 1)
                    this.epagenum = null;
            } catch (Exception e) { }
        } else {
            this.epagenum = -1;
        }
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
