package io.bdrc.iiif.presentation;

public class AppConstants {
    public static final String BDR  = "http://purl.bdrc.io/resource/";
    public static final int BDR_len = BDR.length();
    public static final String BDO  = "http://purl.bdrc.io/ontology/core/";
    public static final String ADM  = "http://purl.bdrc.io/ontology/admin/";
    public static final String TMPPREFIX  = "http://purl.bdrc.io/ontology/tmp/";
    public static final String IIIFPresPrefix = "http://presentation.bdrc.io/2.1.1/";
    public static final String IIIFPresPrefix_coll = IIIFPresPrefix+"collection/";
    
	public static final int GENERIC_APP_ERROR_CODE = 5001;
	
	public static final int INVALID_IDENTIFIER_ERROR_CODE = 5002;
	
	public static final int NO_ACCESS_ERROR_CODE = 5003;
	
	public static final int FILE_LIST_ACCESS_ERROR_CODE = 5004;
	
	public static final int CANNOT_FIND_VOLUME_ERROR_CODE = 5005;
	public static final int CANNOT_FIND_ITEM_ERROR_CODE = 5006;
	public static final int CANNOT_FIND_WORK_ERROR_CODE = 5007;
	
	public static final int GENERIC_LDS_ERROR = 5008;
	public static final int GENERIC_CACHE_ERROR = 5009;

}
