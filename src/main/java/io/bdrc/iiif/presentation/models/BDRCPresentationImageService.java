package io.bdrc.iiif.presentation.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.digitalcollections.iiif.model.PropertyValue;
import de.digitalcollections.iiif.model.image.ImageApiProfile;
import de.digitalcollections.iiif.model.image.ImageService;

public class BDRCPresentationImageService extends ImageService {

    @JsonProperty("preferredFormats")
    private PropertyValue preferredFormats;
    
    public BDRCPresentationImageService(String identifier, ImageApiProfile profile) {
        super(identifier, profile);
    }
    
    public void setPreferredFormats(PropertyValue preferredFormats) {
        this.preferredFormats = preferredFormats;
      }

}
