package io.bdrc.iiif.presentation.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ImageInfo {
    @JsonProperty("width")
    public int width;
    @JsonProperty("height")
    public int height;
    @JsonProperty("filename")
    public String filename;
    @JsonProperty("size")
    public Integer size = null;
    
    public ImageInfo(int width, int height, String filename, Integer size) {
        this.width = width;
        this.height = height;
        this.filename = filename;
        this.size = size;
    }
    
    public ImageInfo() {}
    
    public void setWidth(int width) {
        this.width = width;
    }
    
    public void setHeight(int height) {
        this.height = height;
    }
    
    public void setFilename (String filename) {
        this.filename = filename;
    }
}