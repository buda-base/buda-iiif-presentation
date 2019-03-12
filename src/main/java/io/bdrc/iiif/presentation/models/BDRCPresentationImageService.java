package io.bdrc.iiif.presentation.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.digitalcollections.iiif.model.PropertyValue;
import de.digitalcollections.iiif.model.image.ImageApiProfile;
import de.digitalcollections.iiif.model.image.ImageService;

public class BDRCPresentationImageService extends ImageService {

    @JsonProperty("formatHints")
    private PropertyValue formatHints;
    
    public BDRCPresentationImageService(String identifier, ImageApiProfile profile) {
        super(identifier, profile);
    }
    
    public void setFormatHints(PropertyValue formatHints) {
        this.formatHints = formatHints;
      }

}
