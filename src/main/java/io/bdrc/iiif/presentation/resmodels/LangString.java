package io.bdrc.iiif.presentation.resmodels;

import org.apache.jena.rdf.model.Literal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class LangString {
    @JsonProperty("@value")
    public String value = null;
    @JsonInclude(Include.NON_NULL)
    @JsonProperty("@language")
    public String language = null;

    public LangString(Literal l) {
        this.value = l.getString();
        this.language = l.getLanguage();
    } 
    
    public LangString() {}
}