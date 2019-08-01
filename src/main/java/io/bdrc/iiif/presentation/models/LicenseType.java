package io.bdrc.iiif.presentation.models;

public enum LicenseType {
    COPYRIGHTED("http://purl.bdrc.io/admindata/LicenseCopyrighted"),
    PUBLIC_DOMAIN("http://purl.bdrc.io/admindata/LicensePublicDomain"),
    CCBYSA3("http://purl.bdrc.io/admindata/LicenseCCBYSA3U"),
    CCBYSA4("http://purl.bdrc.io/admindata/LicenseCCBYSA4U"),
    CC0("http://purl.bdrc.io/admindata/LicenseCC0"),
    MIXED("http://purl.bdrc.io/admindata/LicenseMixed");
    
    private String uri;
    
    private LicenseType(final String uri) {
        this.setUri(uri);
    }

    public String getUri() {
        return uri;
    }

    public void setUri(final String uri) {
        this.uri = uri;
    }

    public static LicenseType fromString(final String license) {
        for (LicenseType lt : LicenseType.values()) {
          if (lt.uri.equals(license)) {
            return lt;
          }
        }
        return null;
      }
}
