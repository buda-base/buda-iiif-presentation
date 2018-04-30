package io.bdrc.iiif.presentation.models;

import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import static io.bdrc.iiif.presentation.AppConstants.*;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Identifier {
    public static final int MANIFEST_ID = 0;
    public static final int COLLECTION_ID = 1;
    
    public static final int COLLECTION_ID_ITEM = 2;
    public static final int COLLECTION_ID_WORK_IN_ITEM = 3;
    
    public static final int MANIFEST_ID_WORK_IN_ITEM = 4;
    public static final int MANIFEST_ID_VOLUMEID = 5;
    public static final int MANIFEST_ID_WORK_IN_VOLUMEID = 6;
    
    @JsonProperty("id")
    String id = null;
    @JsonProperty("type")
    int type = -1;
    @JsonProperty("subtype")
    int subtype = -1;
    @JsonProperty("workId")
    String workId = null;
    @JsonProperty("itemId")
    String itemId = null;
    @JsonProperty("volumeId")
    String volumeId = null;
    
    public Identifier(final String iiifIdentifier, final int idType) throws BDRCAPIException {
        if (iiifIdentifier == null || iiifIdentifier.isEmpty())
            throw new BDRCAPIException(404, INVALID_IDENTIFIER_ERROR_CODE, "cannot parse identifier");
        final int firstColIndex = iiifIdentifier.indexOf(':');
        if (firstColIndex < 1)
            throw new BDRCAPIException(404, INVALID_IDENTIFIER_ERROR_CODE, "cannot parse identifier");
        final String typestr = iiifIdentifier.substring(0, firstColIndex);
        final String[] parts = iiifIdentifier.substring(firstColIndex+1).split("::");
        if (parts.length == 0 || parts.length > 2)
            throw new BDRCAPIException(404, INVALID_IDENTIFIER_ERROR_CODE, "cannot parse identifier");
        final String firstId = parts[0];
        if (firstId.isEmpty())
            throw new BDRCAPIException(404, INVALID_IDENTIFIER_ERROR_CODE, "cannot parse identifier");
        final String secondId = (parts.length > 1 && !parts[1].isEmpty()) ? parts[1] : null;
        int nbMaxPartsExpected = 0;
        this.id = iiifIdentifier;
        this.type = idType;
        if (idType == COLLECTION_ID) {
            switch (typestr) {
            case "i":
                this.itemId = firstId;
                nbMaxPartsExpected = 1;
                this.subtype = COLLECTION_ID_ITEM;
                break;
            case "wi":
                this.workId = firstId;
                this.itemId = secondId;
                nbMaxPartsExpected = 2;
                this.subtype = COLLECTION_ID_WORK_IN_ITEM;
                break;
            default:
                throw new BDRCAPIException(404, INVALID_IDENTIFIER_ERROR_CODE, "cannot parse identifier: invalid type \""+typestr+"\"");
            }
            return;
        }
        // idType == MANIFEST_ID
        switch (typestr) {
        case "wi":
            this.workId = firstId;
            this.itemId = secondId;
            nbMaxPartsExpected = 2;
            this.subtype = MANIFEST_ID_WORK_IN_ITEM;
            break;
        case "v":
            this.volumeId = firstId;
            nbMaxPartsExpected = 1;
            this.subtype = MANIFEST_ID_VOLUMEID;
            break;
        case "wv":
            this.workId = firstId;
            this.volumeId = secondId;
            nbMaxPartsExpected = 2;
            this.subtype = MANIFEST_ID_WORK_IN_VOLUMEID;
            break;
        default:
            throw new BDRCAPIException(404, INVALID_IDENTIFIER_ERROR_CODE, "cannot parse identifier: invalid type \""+typestr+"\"");
        }
        if (nbMaxPartsExpected < parts.length)
            throw new BDRCAPIException(404, INVALID_IDENTIFIER_ERROR_CODE, "cannot parse identifier: not enough parts");
        if (!isWellFormedId(workId) || !isWellFormedId(itemId) || !isWellFormedId(volumeId))
            throw new BDRCAPIException(404, INVALID_IDENTIFIER_ERROR_CODE, "cannot parse identifier: ill formed IDs");
    }

    public int getType() {
        return type;
    }

    public int getSubType() {
        return subtype;
    }

    public String getItemId() {
        return itemId;
    }

    public String getVolumeId() {
        return volumeId;
    }

    public String getWorkId() {
        return workId;
    }
    
    public String getId() {
        return id;
    }
    
    // returns false if id is not well formed, returns true on null (for ease of use)
    private boolean isWellFormedId(String id) {
        if (id == null) 
            return true;
        if (id.indexOf('"') != -1 || id.indexOf('\\') != -1 || id.indexOf('\n') != -1)
            return false;
        return true;
    }
    
}
