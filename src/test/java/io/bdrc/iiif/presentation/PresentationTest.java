package io.bdrc.iiif.presentation;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.Test;

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
    
//    @Test
//    public void testManifest() throws BDRCAPIException, JsonGenerationException, JsonMappingException, IOException {
//        Identifier id = new Identifier("ivn:bdr:I22084_I001::0886", Identifier.MANIFEST_ID);
//        //System.out.println(MimeType.fromTypename("image/jpeg"));
//        Manifest m = ManifestService.getManifestForIdentifier(id);
//        IiifObjectMapper mapper = new IiifObjectMapper();
//        mapper.setSerializationInclusion(Include.NON_NULL);
//        File file = new File("/tmp/manifest.json");
//        FileOutputStream fop = new FileOutputStream(file);
//        //mapper.writeValue(fop, m);
//        mapper.writerWithDefaultPrettyPrinter().writeValue(fop, m);
//    }
//
//    @Test
//    public void testCollection() throws BDRCAPIException, JsonGenerationException, JsonMappingException, IOException {
//        Identifier id = new Identifier("i:bdr:I22084_I001", Identifier.COLLECTION_ID);
//        //System.out.println(MimeType.fromTypename("image/jpeg"));
//        Collection c = CollectionService.getCollectionForIdentifier(id);
//        IiifObjectMapper mapper = new IiifObjectMapper();
//        mapper.setSerializationInclusion(Include.NON_NULL);
//        File file = new File("/tmp/collection.json");
//        FileOutputStream fop = new FileOutputStream(file);
//        //mapper.writeValue(fop, m);
//        mapper.writerWithDefaultPrettyPrinter().writeValue(fop, c);
//    }

    
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
    public void testLds() {
        HttpClient httpClient = HttpClientBuilder.create().build(); //Use this instead 
        try {
            HttpPost request = new HttpPost("http://purl.bdrc.io/graph/IIIFPres_workGraph");
            StringEntity params = new StringEntity("{\"R_RES\":\"bdr:W22084\"}", ContentType.APPLICATION_JSON);
            request.addHeader(HttpHeaders.ACCEPT, "text/turtle");
            request.setEntity(params);
            HttpResponse response = httpClient.execute(request);
            int code = response.getStatusLine().getStatusCode();
            if (code != 200) {
                System.out.println("server error!");
                // TODO: do something
                return;
            }
            HttpEntity resEntity = response.getEntity();
            Model m = ModelFactory.createDefaultModel();
            m.read(resEntity.getContent(), null, "TURTLE");
            System.out.println(m.toString());
        }catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
}
