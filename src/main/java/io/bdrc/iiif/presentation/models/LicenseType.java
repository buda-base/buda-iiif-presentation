package io.bdrc.iiif.presentation.models;

public enum LicenseType {
    COPYRIGHTED("http://purl.bdrc.io/resource/LicenseCopyrighted"),
    PUBLIC_DOMAIN("http://purl.bdrc.io/resource/LicensePublicDomain"),
    CCBYSA3("http://purl.bdrc.io/resource/LicenseCCBYSA3U"),
    CCBYSA4("http://purl.bdrc.io/resource/LicenseCCBYSA4U"),
    CC0("http://purl.bdrc.io/resource/LicenseCC0"),
    MIXED("http://purl.bdrc.io/resource/LicenseMixed");
    
    private String uri;
    
    private LicenseType(String uri) {
        this.setUri(uri);
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public static LicenseType fromString(String license) {
        for (LicenseType lt : LicenseType.values()) {
          if (lt.uri.equals(license)) {
            return lt;
          }
        }
        return null;
      }
}
