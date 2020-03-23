package io.bdrc.iiif.presentation.resmodels;

import static io.bdrc.iiif.presentation.AppConstants.GENERIC_APP_ERROR_CODE;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;

import de.digitalcollections.iiif.model.enums.ViewingDirection;

import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;

public class BVM {
    
    public static final class TrueFilter {

        @Override
        public boolean equals(final Object obj) {
            if (obj == null || !(obj instanceof Boolean)) {
                return false;
            }
            final Boolean v = (Boolean) obj;
            return Boolean.TRUE.equals(v);
        }
    }
    
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

        @JsonValue
        public String getLocalName() {
            return this.localName;
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

    public static enum BVMViewingDirection {
        LTR("left-to-right", ViewingDirection.LEFT_TO_RIGHT),
        RTL("right-to-left", ViewingDirection.RIGHT_TO_LEFT),
        TTB("top-to-bottom", ViewingDirection.TOP_TO_BOTTOM);

        private String localName;
        private ViewingDirection iiifViewingDirection;

        private BVMViewingDirection(String localName, ViewingDirection iiifViewingDirection) {
            this.localName = localName;
            this.iiifViewingDirection = iiifViewingDirection;
        }
        
        public ViewingDirection getIIIFViewingDirection() {
            return this.iiifViewingDirection;
        }
        
        @JsonValue
        public String getLocalName() {
            return this.localName;
        }

        public static BVMViewingDirection fromString(String tag) {
            for (BVMViewingDirection at : BVMViewingDirection.values()) {
                if (at.localName.equals(tag)) {
                    return at;
                }
            }
            return null;
        }
    }

    public static enum BVMStatus {
        released("released"),
        editing("editing"),
        withdrawn("withdrawn");

        private String localName;

        private BVMStatus(String localName) {
            this.localName = localName;
        }
        
        @JsonValue
        public String getLocalName() {
            return this.localName;
        }

        public static BVMStatus fromString(String tag) {
            for (BVMStatus at : BVMStatus.values()) {
                if (at.localName.equals(tag)) {
                    return at;
                }
            }
            return null;
        }
    }
    
    public static enum PaginationType {
        folios("folios", Pattern.compile("^\\d+'*[ab]")),
        simple("simple", Pattern.compile("^\\d+")),
        romanlc("romanlc", Pattern.compile("^[ivxlcdm]+"));

        private String localName;
        private Pattern testPattern;

        private PaginationType(String localName, Pattern testPattern) {
            this.localName = localName;
            this.testPattern = testPattern;
        }

        @JsonValue
        public String getLocalName() {
            return this.localName;
        }

        public Pattern getTestPattern() {
            return testPattern;
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
        @JsonProperty(value="user", required=true)
        public String userQname = null;
        @JsonProperty(value="message", required=true)
        public LangString message = null;
        @JsonFormat(timezone = "UTC")
        @JsonProperty(value="time", required=true)
        public Instant time = null;
      
        public ChangeLogItem() { }
        
        public void validate() throws BDRCAPIException {
            if (this.userQname == null || this.message == null || this.time == null)
                throw new BDRCAPIException(422, GENERIC_APP_ERROR_CODE, "invalid bvm: invalid change, missing field");
        }
    }

    public static final class BVMPagination {
        @JsonProperty(value="id", required=true)
        public String id = null;
        @JsonProperty(value="type", required=true)
        public PaginationType type = null;
        @JsonInclude(Include.NON_NULL)
        @JsonProperty("note")
        public List<LangString> note = null;
      
        public BVMPagination() { }
    }

    public static final class BVMSection {
        @JsonProperty(value="id", required=true)
        public String id = null;
        @JsonProperty("name")
        public LangString name = null;
      
        public BVMSection() { }
    }

    public static final class BVMPaginationItem {
        @JsonProperty(value="value", required=true)
        public String value = null;
        @JsonProperty("section")
        public String section = null;
      
        public BVMPaginationItem() { }
        
        public void validate(PaginationType type) throws BDRCAPIException {
            if (!type.getTestPattern().matcher(this.value).matches())
                throw new BDRCAPIException(422, GENERIC_APP_ERROR_CODE, "invalid bvm: invalid pagination value "+this.value);
        }
    }
    
    public static final class BVMImageInfo {
        @JsonInclude(Include.NON_NULL)
        @JsonProperty("tags")
        public List<Tag> tags = null;
        @JsonInclude(Include.NON_NULL)
        @JsonProperty("filename")
        public String filename = null;
        @JsonInclude(Include.NON_NULL)
        @JsonProperty("indication")
        public LangString indication = null;
        @JsonInclude(Include.NON_NULL)
        @JsonProperty("note")
        public List<LangString> note = null;
        @JsonInclude(Include.NON_NULL)
        @JsonProperty("rotation")
        public Integer rotation = null;
        @JsonInclude(Include.NON_NULL)
        @JsonProperty("duplicate-of")
        public String duplicateOf = null;
        @JsonInclude(Include.NON_NULL)
        @JsonProperty("detail-of")
        public String detailOf = null;
        @JsonInclude(value = Include.NON_DEFAULT)
        @JsonProperty(value="hidden")
        public Boolean hidden = Boolean.FALSE;
        @JsonInclude(value = Include.CUSTOM, valueFilter = TrueFilter.class)
        @JsonProperty(value="checked")
        public Boolean checked = Boolean.TRUE;
        @JsonInclude(Include.NON_NULL)
        @JsonProperty("pagination")
        public Map<String,BVMPaginationItem> pagination = null;
        
        public BVMImageInfo() {}
        
        public void validate(final BVM root) throws BDRCAPIException {
            boolean filenameShouldBeEmpty = false;
            if (this.tags != null) {
                for (Tag t : this.tags) {
                    if (t == Tag.T0019 || t == Tag.T0020)
                        filenameShouldBeEmpty = true;
                    //if (t == Tag.T0016 && this.detailOf == null)
                    //    throw new BDRCAPIException(422, GENERIC_APP_ERROR_CODE, "invalid bvm: missing detail-of field");
                    if ((t == Tag.T0017 || t == Tag.T0018) && this.duplicateOf == null)
                        throw new BDRCAPIException(422, GENERIC_APP_ERROR_CODE, "invalid bvm: missing duplicate-of field");
                }
            }
            boolean filenameIsEmpty = (this.filename == null || this.filename.isEmpty());
            if ((filenameShouldBeEmpty && !filenameIsEmpty) || (!filenameShouldBeEmpty && filenameIsEmpty))
                throw new BDRCAPIException(422, GENERIC_APP_ERROR_CODE, "invalid bvm: missing filename or filename on an image tagged as missing");
            if (this.pagination != null) {
                final Map<String, PaginationType> paginationMap = root.getPaginationMap();
                for (final Entry<String,BVMPaginationItem> e : this.pagination.entrySet()) {
                    PaginationType t = paginationMap.get(e.getKey());
                    if (t == null)
                        throw new BDRCAPIException(422, GENERIC_APP_ERROR_CODE, "invalid bvm: invalid pagination type "+e.getKey());
                    e.getValue().validate(t);
                }
            }
        }
        
        @JsonIgnore
        public BVMPaginationItem getDefaultPaginationValue(BVM root) {
            for (final BVMPagination p : root.pagination) {
                if (this.pagination.containsKey(p.id)) {
                    return pagination.get(p.id);
                }
            }
            return null;
        }
        
    }

    public static final class BVMView {
        @JsonProperty(value="imagelist", required=true)
        public List<BVMImageInfo> imageList = null;
        
        public void validate(final BVM root) throws BDRCAPIException {
            if (imageList == null)
                throw new BDRCAPIException(422, GENERIC_APP_ERROR_CODE, "invalid bvm: no image list in view");
            for (BVMImageInfo ii : this.imageList) {
                ii.validate(root);
            }
        }
    }
    
    @JsonProperty(value="rev", required=true)
    public String rev = null;
    @JsonProperty(value="for-volume", required=true)
    public String imageGroupQname = null;
    @JsonProperty(value="spec-version", required=true)
    public String specVersion = null;
    @JsonProperty(value="default-view", required=true)
    public String defaultView = null;
    
    @JsonInclude(Include.NON_NULL)
    @JsonProperty("attribution")
    public List<LangString> attribution = null;
    @JsonInclude(Include.NON_NULL)
    @JsonProperty("note")
    public List<LangString> note = null;
    @JsonInclude(Include.NON_NULL)
    @JsonProperty("volume-label")
    public List<LangString> label = null;
    @JsonInclude(Include.NON_NULL)
    @JsonProperty(value="changes", required=true)
    public List<ChangeLogItem> changes = null;
    @JsonProperty(value="view", required=true)
    public Map<String,BVMView> views = null;
    @JsonInclude(Include.NON_NULL)
    @JsonProperty(value="sections")
    public List<BVMSection> sections = null;
    @JsonProperty(value="pagination", required=true)
    public List<BVMPagination> pagination = null;
    @JsonInclude(Include.NON_NULL)
    @JsonProperty(value="viewing-direction")
    public BVMViewingDirection viewingDirection = null;
    @JsonProperty(value="status")
    public BVMStatus status = null;
    @JsonInclude(Include.NON_NULL)
    @JsonProperty(value="appData")
    public JsonNode appData = null;
    @JsonIgnore
    private List<BVMImageInfo> defaultImageList = null;
    @JsonIgnore
    private Map<String,PaginationType> paginationMap = null;
    @JsonIgnore
    private Map<String,Integer> fnMap = null;
    
    // in the default view
    private void generateFilenameMapDV() {
        this.fnMap = new HashMap<>();
        List<BVMImageInfo> iil = getDefaultImageList();
        int idx = 0;
        for (final BVMImageInfo ii : iil) {
            if (ii.filename != null) {
                this.fnMap.put(ii.filename, idx);
            }
            idx += 1;
        }
    }

    @JsonIgnore
    public Integer getDefaultBVMIndexForFilename(final String filename, final boolean buildListIfMissing) {
        if (this.fnMap == null && buildListIfMissing) {
            generateFilenameMapDV();
        }
        if (this.fnMap == null) {
            List<BVMImageInfo> iil = getDefaultImageList();
            for (int i = 0 ; i < iil.size() ; i++) {
                if (filename.equals(iil.get(i).filename))
                    return i;
            }
            return null;
        } else {
            return this.fnMap.get(filename);
        }
    }
    
    @JsonIgnore
    public List<BVMImageInfo> getDefaultImageList() {
        if (this.defaultImageList != null)
            return this.defaultImageList;
        this.defaultImageList = this.views.get(this.defaultView).imageList;
        return this.defaultImageList;
    }
    
    @JsonIgnore
    public Map<String,PaginationType> getPaginationMap() {
        if (this.paginationMap != null)
            return this.paginationMap;
        this.paginationMap = new HashMap<String,PaginationType>();
        for (final BVMPagination p : this.pagination) {
            this.paginationMap.put(p.id, p.type);
        }
        return this.paginationMap;
    }
    
    @JsonIgnore
    public ChangeLogItem getLatestChangeLogItem() {
        if (this.changes == null)
            return null;
        return this.changes.get(this.changes.size()-1);
    }
    
    public void validate() throws BDRCAPIException {
        if (!"0.1.0".equals(this.specVersion))
            throw new BDRCAPIException(422, GENERIC_APP_ERROR_CODE, "invalid bvm: spec must be 0.1.0");
        if (this.imageGroupQname == null || !this.imageGroupQname.startsWith("bdr:I"))
            throw new BDRCAPIException(422, GENERIC_APP_ERROR_CODE, "invalid bvm: for-volume must start with 'bdr:I'");
        if (this.changes == null)
            throw new BDRCAPIException(422, GENERIC_APP_ERROR_CODE, "invalid bvm: missing changes");
        for (ChangeLogItem ci : this.changes)
            ci.validate();
        if (this.defaultView == null)
            throw new BDRCAPIException(422, GENERIC_APP_ERROR_CODE, "invalid bvm: missing default-view");
        if (!this.views.containsKey(this.defaultView))
            throw new BDRCAPIException(422, GENERIC_APP_ERROR_CODE, "invalid bvm: not view in the view list corresponding to default-view");
        for (BVMView v : this.views.values())
            v.validate(this);
    }
    
}
