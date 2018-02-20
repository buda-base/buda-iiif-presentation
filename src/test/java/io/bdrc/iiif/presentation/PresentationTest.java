package io.bdrc.iiif.presentation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.digitalcollections.core.model.api.MimeType;
import de.digitalcollections.iiif.model.jackson.IiifObjectMapper;
import de.digitalcollections.iiif.model.sharedcanvas.Collection;
import de.digitalcollections.iiif.model.sharedcanvas.Manifest;
import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;

public class PresentationTest {

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
