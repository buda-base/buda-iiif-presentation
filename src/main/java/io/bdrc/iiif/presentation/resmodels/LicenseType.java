package io.bdrc.iiif.presentation.resmodels;

import com.fasterxml.jackson.annotation.JsonValue;

public enum LicenseType {
    COPYRIGHTED("http://purl.bdrc.io/admindata/LicenseCopyrighted", "http://rightsstatements.org/vocab/InC/1.0/"),
    PUBLIC_DOMAIN("http://purl.bdrc.io/admindata/LicensePublicDomain", "http://creativecommons.org/publicdomain/mark/1.0/"),
    CCBYSA3("http://purl.bdrc.io/admindata/LicenseCCBYSA3U", "http://creativecommons.org/licenses/by-sa/3.0/"),
    CCBYSA4("http://purl.bdrc.io/admindata/LicenseCCBYSA4U", "http://creativecommons.org/licenses/by-sa/4.0/"),
    CC0("http://purl.bdrc.io/admindata/LicenseCC0", "http://creativecommons.org/publicdomain/zero/1.0/"),
    MIXED("http://purl.bdrc.io/admindata/LicenseMixed", "http://rightsstatements.org/vocab/UND/1.0/");
    
    private String bdrcUri;
    private String iiifUri;
    
    private LicenseType(final String bdrcUri, final String iiifUri) {
        this.bdrcUri = bdrcUri;
        this.iiifUri = iiifUri;
    }

    @JsonValue
    public String getUri() {
        return bdrcUri;
    }
    
    public String getIIIFUri() {
        return iiifUri;
    }

    public static LicenseType fromString(final String license) {
        for (LicenseType lt : LicenseType.values()) {
          if (lt.bdrcUri.equals(license)) {
            return lt;
          }
        }
        return null;
      }
}
