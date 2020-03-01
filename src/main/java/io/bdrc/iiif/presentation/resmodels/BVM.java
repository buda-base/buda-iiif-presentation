package io.bdrc.iiif.presentation.resmodels;

import static io.bdrc.iiif.presentation.AppConstants.GENERIC_APP_ERROR_CODE;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.apache.jena.rdf.model.Literal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;

public class BVM {
    
    public static enum Tag {
        T0001("T0001"),
        T0002("T0002"),
        T0003("T0003"),
        T0004("T0004"),
        T0005("T0005"),
        T0006("T0006"),
        T0007("T0007"),
        T0008("T0008"),
        T0009("T0009"),
        T0010("T0010"),
        T0011("T0011"),
        T0012("T0012"),
        T0013("T0013"),
        T0014("T0014"),
        T0015("T0015"),
        T0016("T0016"),
        T0017("T0017"),
        T0018("T0018"),
        T0019("T0019"),
        T0020("T0020"),
        T0021("T0021");

        private String localName;

        private Tag(String localName) {
            this.setLocalName(localName);
        }

        public String getLocalName() {
            return localName;
        }

        public void setLocalName(String localName) {
            this.localName = localName;
        }

        public static Tag fromString(String tag) {
            for (Tag at : Tag.values()) {
                if (at.localName.equals(tag)) {
                    return at;
                }
            }
            return null;
        }
    }

    public static enum PaginationType {
        folios("folios"),
        simple("simple");

        private String localName;

        private PaginationType(String localName) {
            this.setLocalName(localName);
        }

        public String getLocalName() {
            return localName;
        }

        public void setLocalName(String localName) {
            this.localName = localName;
        }

        public static PaginationType fromString(String tag) {
            for (PaginationType at : PaginationType.values()) {
                if (at.localName.equals(tag)) {
                    return at;
                }
            }
            return null;
        }
    }
    
    public static final class ChangeLogItem {
        @JsonProperty("user")
        public String userQname = null;
        @JsonProperty("message")
        public LangString message = null;
        @JsonProperty("time")
        public Instant time = null;
      
        public ChangeLogItem(String userQname, LangString message, Instant time) {
            this.userQname = userQname;
            this.message = message;
            this.time = time;
        }
    }

    public static final class BVMImageInfo {
        @JsonInclude(Include.NON_NULL)
        @JsonProperty("tags")
        public List<Tag> tags = null;
        @JsonInclude(Include.NON_NULL)
        @JsonProperty("filename")
        public String filename = null;
        @JsonProperty("indication")
        public LangString indication = null;
      
    }
    
    // class BVMImageInfo
    
    // function validate
    
    // function getDefaultView()
    
    @JsonProperty("rev")
    public UUID rev;
    @JsonProperty("for-volume")
    public String imageGroupQname;
    @JsonProperty("spec-version")
    public String specVersion;
    @JsonProperty("default-view")
    public String defaultView;
    
    @JsonInclude(Include.NON_NULL)
    @JsonProperty("attribution")
    public List<LangString> attribution;
    @JsonInclude(Include.NON_NULL)
    @JsonProperty("note")
    public List<LangString> note;
    @JsonInclude(Include.NON_NULL)
    @JsonProperty("changes")
    public List<ChangeLogItem> changes;
    
    
    
    public void validate() throws BDRCAPIException {
        if (!specVersion.equals("0.1.0")) {
            throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "invalid bvmt: spec must be 0.1.0");
        }
    }
    
}
