package io.bdrc.iiif.presentation.resmodels;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ImageInfoList {
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class ImageInfo {
        @JsonProperty("width")
        public int width;
        @JsonProperty("height")
        public int height;
        @JsonProperty("filename")
        public String filename;
        @JsonInclude(Include.NON_NULL)
        @JsonProperty("size")
        public Integer size = null;
        
        public ImageInfo(int width, int height, String filename, Integer size) {
            this.width = width;
            this.height = height;
            this.filename = filename;
            this.size = size;
        }
        
        public ImageInfo() {}
        
        @Override
        public String toString() {
            try {
                return new ObjectMapper().writeValueAsString(this);
            } catch (JsonProcessingException e) {
                return "toString objectmapper exception, this shouldn't happen";
            }
        }
    }
    
    public List<ImageInfo> list = null;
    public Map<String,ImageInfo> map = null;
    
    public ImageInfoList(List<ImageInfo> list) {
        list.removeIf(imageInfo -> imageInfo.filename.endsWith("json"));
        this.list = list;
    }
    
    private void generateMap() {
        this.map = new HashMap<>();
        for (final ImageInfo ii: this.list) {
            this.map.put(ii.filename, ii);
        }
    }
    
    public ImageInfo getFromFilename(final String fn) {
        if (this.map == null)
            generateMap();
        return this.map.get(fn);
    }
 
    public Integer getSeqNumFromFilename(final String fn) {
        // we don't build the map for this one
        int res = 1; // seqNum starts at 1
        for (final ImageInfo i : this.list) {
            if (i.filename.equals(fn))
                return res;
            res += 1;
        }
        return null;
    }
    
    public ImageInfo get(final int i) {
        return this.list.get(i);
    }
    
    public int size() {
        return this.list.size();
    }
}