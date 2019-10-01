package io.bdrc.iiif.presentation.resservices;

import static io.bdrc.iiif.presentation.AppConstants.CACHEPREFIX_IIL;
import static io.bdrc.iiif.presentation.AppConstants.GENERIC_APP_ERROR_CODE;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

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
import io.bdrc.iiif.presentation.resmodels.ImageInfo;

public class ImageInfoListService extends ConcurrentResourceService<List<ImageInfo>> {

	final static ObjectMapper mapper = new ObjectMapper();
	final static String bucketName = "archive.tbrc.org";
	private static AmazonS3 s3Client = null;
	static MessageDigest md;
	static private final ObjectMapper om;
	private static final Logger logger = LoggerFactory.getLogger(ImageInfoListService.class);
	private static final Charset utf8 = Charset.forName("UTF-8");
	public static final ImageInfoListService Instance = new ImageInfoListService();

	ImageInfoListService() {
		super(CACHEPREFIX_IIL);
	}

	static {
		om = new ObjectMapper();
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			logger.error("this shouldn't happen!", e);
		}
	}

	private static String getFirstMd5Nums(final String workLocalId) {
		final byte[] bytesOfMessage;
		bytesOfMessage = workLocalId.getBytes(utf8);
		final byte[] hashBytes = md.digest(bytesOfMessage);
		final BigInteger bigInt = new BigInteger(1, hashBytes);
		return String.format("%032x", bigInt).substring(0, 2);
	}

	private static AmazonS3 getClient() {
		if (s3Client == null) {
			AmazonS3ClientBuilder clientBuilder = AmazonS3ClientBuilder.standard().withRegion(AuthProps.getProperty("awsRegion"));
			s3Client = clientBuilder.build();
		}
		return s3Client;
	}

	public static String getKey(final String workLocalId, String imageGroupId) {
		final String md5firsttwo = getFirstMd5Nums(workLocalId);
		imageGroupId = getS3ImageGroupId(imageGroupId);
		return "Works/" + md5firsttwo + "/" + workLocalId + "/images/" + workLocalId + "-" + imageGroupId + "/dimensions.json";
	}

	public static final Pattern oldImageGroupPattern = Pattern.compile("^I\\d{4}$");

	// for image groups like I\d\d\d\d, the s3 key doesn't contain the I (ex: I0886
	// -> 0886)
	public static String getS3ImageGroupId(final String dataImageGroupId) {
		if (oldImageGroupPattern.matcher(dataImageGroupId).matches())
			return dataImageGroupId.substring(1);
		return dataImageGroupId;
	}

	public final CompletableFuture<List<ImageInfo>> getAsync(final String workLocalId, final String imageGroupId) throws BDRCAPIException {
		final String s3key = getKey(workLocalId, imageGroupId);
		return getAsync(s3key);
	}

	@Override
	final public List<ImageInfo> getFromApi(final String s3key) throws BDRCAPIException {
		final AmazonS3 s3Client = getClient();
		logger.info("fetching s3 key {}", s3key);
		final S3Object object;
		try {
			object = s3Client.getObject(new GetObjectRequest(bucketName, s3key));
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
			final List<ImageInfo> imageList = om.readValue(gis, new TypeReference<List<ImageInfo>>() {});
			objectData.close();
			imageList.removeIf(imageInfo -> imageInfo.filename.endsWith("json"));
			return imageList;
		} catch (IOException e) {
			throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, e);
		}
	}

}
