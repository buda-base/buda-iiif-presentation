package io.bdrc.iiif.presentation;

import static io.bdrc.iiif.presentation.AppConstants.FILE_LIST_ACCESS_ERROR_CODE;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.ws.rs.core.Application;

import org.junit.Test;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.digitalcollections.core.model.api.MimeType;
import de.digitalcollections.iiif.model.jackson.IiifObjectMapper;
import de.digitalcollections.iiif.model.sharedcanvas.Collection;
import de.digitalcollections.iiif.model.sharedcanvas.Manifest;
import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;

public class PresentationTest {

    
//    @Test
//    public void testS3() throws IOException {
//        AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();   
//        S3Object object = s3Client.getObject(
//                          new GetObjectRequest("archive.tbrc.org", "Works/aa/W22704/images/W22704-3371/dimensions.json"));
//        InputStream objectData = object.getObjectContent();
//        GZIPInputStream gis = new GZIPInputStream(objectData);
//        ObjectMapper om = new ObjectMapper();
//        final List<ImageInfo> imageList = om.readValue(gis, new TypeReference<List<ImageInfo>>(){});
//        om.writerWithDefaultPrettyPrinter().writeValue(System.out, imageList);
//        objectData.close();
//    }
    
    @Test
    public void testManifest() throws BDRCAPIException, JsonGenerationException, JsonMappingException, IOException {
        Identifier id = new Identifier("ivn:bdr:I22084_I001::0886", Identifier.MANIFEST_ID);
        //System.out.println(MimeType.fromTypename("image/jpeg"));
        Manifest m = ManifestService.getManifestForIdentifier(id);
        IiifObjectMapper mapper = new IiifObjectMapper();
        mapper.setSerializationInclusion(Include.NON_NULL);
        File file = new File("/tmp/manifest.json");
        FileOutputStream fop = new FileOutputStream(file);
        //mapper.writeValue(fop, m);
        mapper.writerWithDefaultPrettyPrinter().writeValue(fop, m);
    }

    @Test
    public void testCollection() throws BDRCAPIException, JsonGenerationException, JsonMappingException, IOException {
        Identifier id = new Identifier("i:bdr:I22084_I001", Identifier.COLLECTION_ID);
        //System.out.println(MimeType.fromTypename("image/jpeg"));
        Collection c = CollectionService.getCollectionForIdentifier(id);
        IiifObjectMapper mapper = new IiifObjectMapper();
        mapper.setSerializationInclusion(Include.NON_NULL);
        File file = new File("/tmp/collection.json");
        FileOutputStream fop = new FileOutputStream(file);
        //mapper.writeValue(fop, m);
        mapper.writerWithDefaultPrettyPrinter().writeValue(fop, c);
    }

    
//    @Test
//    public void testAll() throws BDRCAPIException, JsonGenerationException, JsonMappingException, IOException {
//        // 886 - 988
//        for (int i = 886 ; i < 989 ; i++) {
//            Identifier id = new Identifier("ivn:bdr:I22084_I001::0"+i, Identifier.MANIFEST_ID);
//            //System.out.println(MimeType.fromTypename("image/jpeg"));
//            Manifest m = ManifestService.getManifestForIdentifier(id);
//            IiifObjectMapper mapper = new IiifObjectMapper();
//            mapper.setSerializationInclusion(Include.NON_NULL);
//            File file = new File("/tmp/manifest-0"+i+".json");
//            FileOutputStream fop = new FileOutputStream(file);
//            //mapper.writeValue(fop, m);
//            mapper.writerWithDefaultPrettyPrinter().writeValue(fop, m);
//        }
//    }

    @Test
    public void testMime() {
        System.out.println(MimeType.fromTypename("image/jpeg"));
    }
    
}
