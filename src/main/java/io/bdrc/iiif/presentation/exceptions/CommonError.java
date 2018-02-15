package io.bdrc.iiif.presentation.exceptions;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CommonError {
    @JsonProperty("code")
    public String code;
    @JsonProperty("message")
    public String message;
    
    public CommonError(String code, String message) {
    	this.code = code;
    	this.message = message;
    }
}
