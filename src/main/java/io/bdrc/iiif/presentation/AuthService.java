package io.bdrc.iiif.presentation;

import io.bdrc.iiif.presentation.models.Identifier;

// currently static stuff, should be turned into an actual service

public class AuthService {
    
    public static enum AuthType {
        ACCESS_NONE,
        ACCESS_LIMITTED,
        ACCESS_FULL;
    }
    
    public static AuthType getAccessForIdentifier(Identifier id)  {
        System.out.println(id.getItemId());
        if (id.getItemId() != null && id.getItemId().equals("bdr:I22084_I001")) {
            return AuthType.ACCESS_FULL;
        }
        return AuthType.ACCESS_NONE;
    }
}
