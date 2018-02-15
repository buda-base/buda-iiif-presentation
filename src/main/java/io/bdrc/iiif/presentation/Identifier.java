package io.bdrc.iiif.presentation;

import org.apache.jena.rdf.model.Resource;

import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import static io.bdrc.iiif.presentation.AppConstants.*;

public class Identifier {
    public static final int MANIFEST_ID = 0;
    public static final int COLLECTION_ID = 1;
    
    public static final int COLLECTION_ID_ITEM = 2;
    public static final int COLLECTION_ID_WORK_IN_ITEM = 3;
    
    public static final int MANIFEST_ID_WORK_IN_ITEM = 4;
    public static final int MANIFEST_ID_VOLUMEID = 5;
    public static final int MANIFEST_ID_WORK_IN_VOLUMEID = 6;
    public static final int MANIFEST_ID_WORK_IN_ITEM_VOLNUM = 7;
    public static final int MANIFEST_ID_ITEM_VOLNUM = 8;
    
    int type = -1;
    Resource work = null;
    Resource item = null;
    Resource volume = null;
    int volNum = 0;
    
    public Identifier(String iiifIdentifier, int idType) throws BDRCAPIException {
        if (iiifIdentifier == null || iiifIdentifier.isEmpty())
            throw new BDRCAPIException(404, INVALID_IDENTIFIER_ERROR_CODE, "cannot parse identifier");
        int firstColIndex = iiifIdentifier.indexOf(':');
        if (firstColIndex < 1)
            throw new BDRCAPIException(404, INVALID_IDENTIFIER_ERROR_CODE, "cannot parse identifier");
        final String typestr = iiifIdentifier.substring(0, firstColIndex);
        int secondColIndex = iiifIdentifier.indexOf("::", firstColIndex+1);
        final String firstIdentifier;
        String secondIdentifier = null;
        String thirdIdentifier = null;
        int identifierLength = iiifIdentifier.length();
        if (secondColIndex == -1) {
            if (firstColIndex+1 >= identifierLength)
                throw new BDRCAPIException(404, INVALID_IDENTIFIER_ERROR_CODE, "cannot parse identifier");
            firstIdentifier = iiifIdentifier.substring(firstColIndex+1);
        } else {
            firstIdentifier = null; // TODO
        }
        
        if (idType == COLLECTION_ID) {
            switch (typestr) {
            case "i":
                type = COLLECTION_ID_ITEM;
                break;
            case "wi":
                type = COLLECTION_ID_WORK_IN_ITEM;
                break;
            default:
                throw new BDRCAPIException(404, INVALID_IDENTIFIER_ERROR_CODE, "cannot parse identifier");
            }
            return;
        }
        // idType == MANIFEST_ID
        switch (typestr) {
        case "wi":
            type = MANIFEST_ID_WORK_IN_ITEM;
            break;
        case "v":
            type = MANIFEST_ID_VOLUMEID;
            break;
        case "wv":
            type = MANIFEST_ID_WORK_IN_VOLUMEID;
            break;
        case "wivn":
            type = MANIFEST_ID_WORK_IN_ITEM_VOLNUM;
            break;
        case "ivn":
            type = MANIFEST_ID_ITEM_VOLNUM;
            break;
        default:
            throw new BDRCAPIException(404, INVALID_IDENTIFIER_ERROR_CODE, "cannot parse identifier");
        }
    }
    
}
