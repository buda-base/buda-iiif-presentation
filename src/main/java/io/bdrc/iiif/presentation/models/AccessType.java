package io.bdrc.iiif.presentation.models;

public enum AccessType {
    FAIR_USE("http://purl.bdrc.io/admindata/AccessFairUse"),
    OPEN("http://purl.bdrc.io/admindata/AccessOpen"),
    MIXED("http://purl.bdrc.io/admindata/AccessMixed"), 
    RESTR_QUALITY("http://purl.bdrc.io/admindata/AccessRestrictedByQuality"),
    RESTR_BDRC("http://purl.bdrc.io/admindata/AccessRestrictedByTbrc"), 
    RESTR_SEALED("http://purl.bdrc.io/admindata/AccessRestrictedSealed"),
    RESTR_TEMP("http://purl.bdrc.io/admindata/AccessRestrictedTemporarily");

    private String uri;

    private AccessType(String uri) {
        this.setUri(uri);
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public static AccessType fromString(String access) {
        for (AccessType at : AccessType.values()) {
            if (at.uri.equals(access)) {
                return at;
            }
        }
        return null;
    }
}