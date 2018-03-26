package io.bdrc.iiif.presentation.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ImageInfo {
    @JsonProperty("width")
    public int width;
    @JsonProperty("height")
    public int height;
    @JsonProperty("filename")
    public String filename;
    
    public ImageInfo(int width, int height, String filename) {
        this.width = width;
        this.height = height;
        this.filename = filename;
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