package io.bdrc.iiif.presentation.models;

import org.apache.jena.rdf.model.Literal;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LangString {
    @JsonProperty("@value")
    public String value;
    @JsonProperty("@language")
    public String language;

    public LangString(Literal l) {
        this.value = l.getString();
        this.language = l.getLanguage();
    }
}