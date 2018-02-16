package io.bdrc.iiif.presentation;

// currently static stuff, should be turned into an actual service

public class AuthService {
    
    public static final int ACCESS_NONE = 0;
    public static final int ACCESS_LIMITTED = 1;
    public static final int ACCESS_FULL = 2;
    
    static int getIdentifierAccess(Identifier id)  {
        if (id.itemId != null && id.itemId == "bdr:I22084_I001") {
            return ACCESS_FULL;
        }
        return ACCESS_NONE;
    }
}
