package io.bdrc.iiif.presentation;

import static io.bdrc.iiif.presentation.AppConstants.GENERIC_APP_ERROR_CODE;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.commons.jcs.access.CacheAccess;
import org.apache.commons.jcs.access.exception.CacheException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.auth.AuthProps;
import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.models.ImageInfo;

public class ImageInfoListService {

    final static ObjectMapper mapper = new ObjectMapper();
    final static String bucketName = "archive.tbrc.org";
    private static AmazonS3 s3Client = null;
    static MessageDigest md;
    private static CacheAccess<String, Object> cache = null;
    static private final ObjectMapper om;
    private static final Logger logger = LoggerFactory.getLogger(ImageInfoListService.class);
    private static final Charset utf8 = Charset.forName("UTF-8");

    static {
        om = new ObjectMapper();
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            logger.error("this shouldn't happen!", e);
        }
        try {
            cache = ServiceCache.CACHE;
        } catch (CacheException e) {
            logger.error("cache initialization error, this shouldn't happen!", e);
        }
    }

    private static String getFirstMd5Nums(final String workLocalId) {
        final byte[] bytesOfMessage;
        bytesOfMessage = workLocalId.getBytes(utf8);
        final byte[] hashBytes = md.digest(bytesOfMessage);
        final BigInteger bigInt = new BigInteger(1,hashBytes);
        return String.format("%032x", bigInt).substring(0, 2);
    }

    private static AmazonS3 getClient() {
        if (s3Client == null) {
            AmazonS3ClientBuilder clientBuilder = AmazonS3ClientBuilder.standard().withRegion(AuthProps.getProperty("awsRegion"));
            s3Client=clientBuilder.build();
        }
        return s3Client;
    }

    private static String getKey(final String workLocalId, final String imageGroupId) {
        final String md5firsttwo = getFirstMd5Nums(workLocalId);
        return "Works/"+md5firsttwo+"/"+workLocalId+"/images/"+workLocalId+"-"+imageGroupId+"/dimensions.json";
    }

    private static List<ImageInfo> getFromS3(final String workLocalId, final String imageGroupId) throws BDRCAPIException  {
        final AmazonS3 s3Client = getClient();
        final String key = getKey(workLocalId, imageGroupId);
        logger.info("fetching s3 key {}", key);
        final S3Object object;
        try {
            object = s3Client.getObject(new GetObjectRequest(bucketName, key));
        } catch (AmazonS3Exception e) {
            if (e.getErrorCode().equals("NoSuchKey")) {
                throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "sorry, BDRC did not complete the data migration for this Work");
            } else {
                throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, e);
            }
        }
        final InputStream objectData = object.getObjectContent();
        try {
            final GZIPInputStream gis = new GZIPInputStream(objectData);
            final List<ImageInfo> imageList = om.readValue(gis, new TypeReference<List<ImageInfo>>(){});
            objectData.close();
            return imageList;
        } catch (IOException e) {
            throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, e);
        }
    }

    public static final Pattern oldImageGroupPattern = Pattern.compile("^I\\d{4}$");
    // for image groups like I\d\d\d\d, the s3 key doesn't contain the I (ex: I0886 -> 0886)
    public static String getS3ImageGroupId(final String dataImageGroupId) {
        if (oldImageGroupPattern.matcher(dataImageGroupId).matches())
            return dataImageGroupId.substring(1);
        return dataImageGroupId;
    }

    @SuppressWarnings("unchecked")
    public static List<ImageInfo> getImageInfoList(final String workLocalId, String imageGroupId) throws BDRCAPIException {
        logger.debug("getting imageInfoList for {}, {}", workLocalId, imageGroupId);
        imageGroupId = getS3ImageGroupId(imageGroupId);
        final String cacheKey = workLocalId+'/'+imageGroupId;
        List<ImageInfo> imageInfoList = (List<ImageInfo>)cache.get(cacheKey);
        if (imageInfoList != null) {
            logger.debug("found in cache");
            return imageInfoList;
        }
        imageInfoList = getFromS3(workLocalId, imageGroupId);
        if (imageInfoList == null)
            throw new BDRCAPIException(500, AppConstants.GENERIC_LDS_ERROR, "Cannot retrieve image list from s3");
        cache.put(cacheKey, imageInfoList);
        return imageInfoList;
    }
}
