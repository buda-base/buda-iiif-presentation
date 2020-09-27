package io.bdrc.iiif.presentation;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.bdrc.libraries.IdentifierException;

public class Identifier {

    public static final int INVALID_IDENTIFIER_ERROR_CODE = 5002;

    public static final int MANIFEST_ID = 0;
    public static final int COLLECTION_ID = 1;

    public static final int COLLECTION_ID_ITEM = 2;
    public static final int COLLECTION_ID_WORK_IN_ITEM = 3;
    public static final int COLLECTION_ID_WORK_OUTLINE = 7;
    public static final int COLLECTION_ID_ITEM_VOLUME_OUTLINE = 9;

    public static final int MANIFEST_ID_WORK_IN_ITEM = 4;
    public static final int MANIFEST_ID_VOLUMEID = 5;
    public static final int MANIFEST_ID_WORK_IN_VOLUMEID = 6;
    public static final int MANIFEST_ID_VOLUMEID_OUTLINE = 8;
    public static final int MANIFEST_ID_WORK_IN_VOLUMEID_OUTLINE = 10;
    
    final static int INSTANCE_ID = 100;
    final static int IMAGEINSTANCE_ID = 101;
    final static int IMAGEGROUP_ID = 102;

    public final static int RANGE_NORANGE = 0;
    public final static int RANGE_PAGENUM = 1;
    public final static int RANGE_PAGEFILENAME = 2;
    
    @JsonProperty("id")
    String id = null;
    @JsonProperty("type")
    int type = -1;
    @JsonProperty("subtype")
    int subtype = -1;
    @JsonProperty("instanceId")
    String instanceId = null;
    @JsonProperty("imageInstanceId")
    String imageInstanceId = null;
    @JsonProperty("imageGroupId")
    String imageGroupId = null;
    @JsonProperty("pageRangeType")
    Integer pageRangeType = null;
    @JsonProperty("bPageNum")
    Integer bPageNum = null;
    @JsonProperty("ePageNum")
    Integer ePageNum = null;
    @JsonProperty("bPageFileName")
    Integer bPageFileName = null;
    @JsonProperty("ePageFileName")
    Integer ePageFileName = null;

    public String transitionToNew(String requestedId, final int type) {
        if (type == INSTANCE_ID) {
            //if (requestedId != null && requestedId.startsWith("bdr:W") && !requestedId.startsWith("bdr:W1ERI"))
            //    return "bdr:MW"+requestedId.substring(5);
        } else if (type == IMAGEINSTANCE_ID) {
            if (requestedId != null && requestedId.startsWith("bdr:I"))
                return "bdr:W"+requestedId.substring(5);
        } else if (requestedId != null && requestedId.startsWith("bdr:V")) {
                int uIidx = requestedId.lastIndexOf("_I");
                if (uIidx != -1)
                    return "bdr:"+requestedId.substring(uIidx + 1);
        }
        return requestedId;
    }
    
    public void setPageNumFromIdPart(final String idPart) throws IdentifierException {
        if (idPart == null || idPart.isEmpty())
            return;
        final String[] parts = idPart.split("-");
        if (parts.length == 0 || parts.length > 3) {
            throw new IdentifierException("cannot parse page numbers in identifier");
        }
        if (!parts[0].isEmpty()) { // case of "-12"
            try {
                this.bPageNum = Integer.parseInt(parts[0]);
            } catch (NumberFormatException e) {
                throw new IdentifierException("cannot parse page numbers in identifier");
            }
            if (this.bPageNum < 1)
                throw new IdentifierException("cannot parse page numbers in identifier");
        }
        if (parts.length < 2)
            return;
        try {
            this.ePageNum = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            throw new IdentifierException("cannot parse page numbers in identifier");
        }
        if (this.ePageNum < 1)
            throw new IdentifierException("cannot parse page numbers in identifier");
    }

    public Identifier(final String iiifIdentifier, final int idType) throws IdentifierException {
        if (iiifIdentifier == null || iiifIdentifier.isEmpty())
            throw new IdentifierException("cannot parse identifier");
        final int firstColIndex = iiifIdentifier.indexOf(':');
        if (firstColIndex < 1)
            throw new IdentifierException("cannot parse identifier");
        final String typestr = iiifIdentifier.substring(0, firstColIndex);
        final String[] parts = iiifIdentifier.substring(firstColIndex + 1).split("::");
        if (parts.length == 0 || parts.length > 3)
            throw new IdentifierException("cannot parse identifier");
        final String firstId = parts[0];
        if (firstId.isEmpty())
            throw new IdentifierException("cannot parse identifier");
        final String secondId = (parts.length > 1 && !parts[1].isEmpty()) ? parts[1] : null;
        final String thirdId = (parts.length > 2 && !parts[2].isEmpty()) ? parts[2] : null;
        int nbMaxPartsExpected = 0;
        this.id = iiifIdentifier;
        this.type = idType;
        if (idType == COLLECTION_ID) {
            switch (typestr) {
            case "i":
                this.imageInstanceId = transitionToNew(firstId, IMAGEINSTANCE_ID);
                nbMaxPartsExpected = 1;
                this.subtype = COLLECTION_ID_ITEM;
                break;
            case "ivo":
                this.imageInstanceId = transitionToNew(firstId, IMAGEINSTANCE_ID);
                nbMaxPartsExpected = 1;
                this.subtype = COLLECTION_ID_ITEM_VOLUME_OUTLINE;
                break;
            case "wi":
                this.instanceId = transitionToNew(firstId, INSTANCE_ID);
                this.imageInstanceId = transitionToNew(secondId, IMAGEINSTANCE_ID);
                nbMaxPartsExpected = 2;
                this.subtype = COLLECTION_ID_WORK_IN_ITEM;
                break;
            case "wio":
                this.instanceId = transitionToNew(firstId, INSTANCE_ID);
                this.imageInstanceId = transitionToNew(secondId, IMAGEINSTANCE_ID);
                nbMaxPartsExpected = 2;
                this.subtype = COLLECTION_ID_WORK_OUTLINE;
                break;
            default:
                throw new IdentifierException("cannot parse identifier: invalid type \"" + typestr + "\"");
            }
            return;
        }
        // idType == MANIFEST_ID
        switch (typestr) {
        case "wi":
            this.instanceId = transitionToNew(firstId, INSTANCE_ID);
            this.imageInstanceId = transitionToNew(secondId, IMAGEINSTANCE_ID);
            setPageNumFromIdPart(thirdId);
            nbMaxPartsExpected = 3;
            this.subtype = MANIFEST_ID_WORK_IN_ITEM;
            break;
        case "v":
            this.imageGroupId = transitionToNew(firstId, IMAGEGROUP_ID);
            setPageNumFromIdPart(secondId);
            nbMaxPartsExpected = 2;
            this.subtype = MANIFEST_ID_VOLUMEID;
            break;
        case "vo":
            this.imageGroupId = transitionToNew(firstId, IMAGEGROUP_ID);
            nbMaxPartsExpected = 1;
            this.subtype = MANIFEST_ID_VOLUMEID_OUTLINE;
            break;
        case "wv":
            this.instanceId = transitionToNew(firstId, INSTANCE_ID);
            this.imageGroupId = transitionToNew(secondId, IMAGEGROUP_ID);
            nbMaxPartsExpected = 2;
            this.subtype = MANIFEST_ID_WORK_IN_VOLUMEID;
            break;
        case "wvo":
            this.instanceId = transitionToNew(firstId, INSTANCE_ID);
            this.imageGroupId = transitionToNew(secondId, IMAGEGROUP_ID);;
            nbMaxPartsExpected = 2;
            this.subtype = MANIFEST_ID_WORK_IN_VOLUMEID_OUTLINE;
            break;
        default:
            throw new IdentifierException("cannot parse identifier: invalid type \"" + typestr + "\"");
        }
        if (nbMaxPartsExpected < parts.length)
            throw new IdentifierException("cannot parse identifier: not enough parts");
        if (!isWellFormedId(instanceId) || !isWellFormedId(imageInstanceId) || !isWellFormedId(imageGroupId))
            throw new IdentifierException("cannot parse identifier: ill formed IDs");
    }

    public int getType() {
        return type;
    }

    public int getSubType() {
        return subtype;
    }

    public String getImageInstanceId() {
        return imageInstanceId;
    }

    public String getImageGroupId() {
        return imageGroupId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getId() {
        return id;
    }

    public Integer getBPageNum() {
        return bPageNum;
    }

    public Integer getEPageNum() {
        return ePageNum;
    }

    // returns false if id is not well formed, returns true on null (for ease of
    // use)
    private boolean isWellFormedId(String id) {
        if (id == null)
            return true;
        if (id.indexOf('"') != -1 || id.indexOf('\\') != -1 || id.indexOf('\n') != -1)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Identifier [id=" + id + ", type=" + type + ", subtype=" + subtype + ", instanceId=" + instanceId + ", imageInstanceId=" + imageInstanceId + ", imageGroupId=" + imageGroupId + ", bPageNum=" + bPageNum + ", ePageNum=" + ePageNum + "]";
    }

}
