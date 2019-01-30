package io.bdrc.iiif.presentation;

public class AppConstants {
    public static final String BDR  = "http://purl.bdrc.io/resource/";
    public static final int BDR_len = BDR.length();
    public static final String BDO  = "http://purl.bdrc.io/ontology/core/";
    public static final String ADM  = "http://purl.bdrc.io/ontology/admin/";
    public static final String TMPPREFIX  = "http://purl.bdrc.io/ontology/tmp/";
    public static final String IIIFPresPrefix = "http://presentation.bdrc.io/2.1.1/";
    public static final String IIIFPresPrefix_coll = IIIFPresPrefix+"collection/";

    public static final String LDS_QUERYPREFIX = "http://buda1.bdrc.io/";
    public static final String LDS_WORKGRAPH_QUERY = LDS_QUERYPREFIX+"graph/IIIFPres_workGraph_noItem";
    public static final String LDS_ITEMGRAPH_QUERY = LDS_QUERYPREFIX+"graph/IIIFPres_itemGraph";
    public static final String LDS_VOLUME_QUERY = LDS_QUERYPREFIX+"query/IIIFPres_volumeInfo";
    public static final String LDS_VOLUME_OUTLINE_QUERY = LDS_QUERYPREFIX+"query/IIIFPres_volumeOutline";

    //public static final String IIIF_IMAGE_PREFIX = "http://iiif.bdrc.io/image/v2/";
    public static final String IIIF_IMAGE_PREFIX = "http://iiif.bdrc.io/";
    public static final String PDF_URL_PREFIX = IIIF_IMAGE_PREFIX+"download/pdf/";
    public static final String ZIP_URL_PREFIX = IIIF_IMAGE_PREFIX+"download/zip/";

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
