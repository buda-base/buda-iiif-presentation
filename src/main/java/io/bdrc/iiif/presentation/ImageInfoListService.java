package io.bdrc.iiif.presentation;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.commons.jcs.JCS;
import org.apache.commons.jcs.access.CacheAccess;
import org.apache.commons.jcs.access.exception.CacheException;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ImageInfoListService {

    final static String prefix = "/home/eroux/BUDA/softs/METStodim/output/60_W22084_W22084-";
    final static ObjectMapper mapper = new ObjectMapper();
    final static String bucketName = "archive.tbrc.org";
    private static AmazonS3 s3Client = null;
    static MessageDigest md;
    private static CacheAccess<String, List<ImageInfo>> cache = null;
    static private final ObjectMapper om;
    
    static {
        om = new ObjectMapper();
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            // silliness...
        }
        try {
            cache = JCS.getInstance("default");
        } catch (CacheException e) {
            // hmm....
        }
    }
    
    private static String getFirstMd5Nums(final String workId) {
        final byte[] bytesOfMessage;
        try {
            bytesOfMessage = workId.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
        final byte[] hashBytes = md.digest(bytesOfMessage);
        final BigInteger bigInt = new BigInteger(1,hashBytes);
        return String.format("%032x", bigInt).substring(0, 2);
    }
    
    private static AmazonS3 getClient() {
        if (s3Client == null)
            s3Client = AmazonS3ClientBuilder.defaultClient();
        return s3Client;
    }
    
    private static String getKey(final String workId, final String imageGroupId) {
        final String md5firsttwo = getFirstMd5Nums(workId);
        return "Works/"+md5firsttwo+"/"+workId+"/images/"+workId+"-"+imageGroupId+"/dimensions.json";
    }
    
    private static List<ImageInfo> getFromS3(final String workId, final String imageGroupId)  {
        final AmazonS3 s3Client = getClient();
        final String key = getKey(workId, imageGroupId);
        final S3Object object;
        try {
            object = s3Client.getObject(new GetObjectRequest(bucketName, key));
        } catch (AmazonS3Exception e) {
            return null;
        }
        final InputStream objectData = object.getObjectContent();
        try {
            final GZIPInputStream gis = new GZIPInputStream(objectData);
            final List<ImageInfo> imageList = om.readValue(gis, new TypeReference<List<ImageInfo>>(){});
            objectData.close();
            return imageList;
        } catch (IOException e) {
            return null;
        }
    }
    
    static List<ImageInfo> getImageInfoList(final String workId, final String imageGroupId) {
        final String cacheKey = workId+'/'+imageGroupId;
        List<ImageInfo> imageInfoList = cache.get(cacheKey);
        if (imageInfoList != null) {
            return imageInfoList;
        }
        imageInfoList = getFromS3(workId, imageGroupId);
        if (imageInfoList == null)
            return null;
        cache.put(cacheKey, imageInfoList);
        return imageInfoList;
    }
}
