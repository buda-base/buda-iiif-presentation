package io.bdrc.iiif.presentation;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.spi.IIORegistry;

import org.apache.commons.jcs.access.CacheAccess;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RDFParserBuilder;
import org.apache.jena.riot.system.StreamRDFLib;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.digitalcollections.iiif.model.sharedcanvas.Manifest;
import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.models.Identifier;
import io.bdrc.iiif.presentation.models.ImageInfo;
import io.bdrc.iiif.presentation.models.ItemInfo;
import io.bdrc.iiif.presentation.models.VolumeInfo;
import io.bdrc.iiif.presentation.models.WorkInfo;


public class PresentationTest {

    final static String TESTDIR = "src/test/resources/";
    final static ObjectMapper om = new ObjectMapper();
    
    @BeforeClass
    public static void before() {
        ServiceCache.init();
          Iterator<ImageWriter> imageWriters = ImageIO.getImageWritersByMIMEType("image/jpeg");
          for (Iterator iterator = imageWriters; iterator.hasNext(); ) {
            ImageWriter imageWriter = (ImageWriter) iterator.next();
                System.out.println(imageWriter.getClass());
          }
    }
    
    @Test
    public void itemInfoModelTest() throws BDRCAPIException, JsonGenerationException, JsonMappingException, IOException {
        Model m = ModelFactory.createDefaultModel();
        RDFParserBuilder pb = RDFParser.create()
                .source(TESTDIR+"itemInfoGraph.ttl")
                .lang(RDFLanguages.TTL);
                //.canonicalLiterals(true);
        pb.parse(StreamRDFLib.graph(m.getGraph()));
        ItemInfo itemInfo = new ItemInfo(m, "bdr:I22083");
        om.writeValue(System.out, itemInfo);
    }

    @Test
    public void workInfoModelTest() throws BDRCAPIException, JsonGenerationException, JsonMappingException, IOException {
        Model m = ModelFactory.createDefaultModel();
        RDFParserBuilder pb = RDFParser.create()
                .source(TESTDIR+"workGraphItem.ttl")
                .lang(RDFLanguages.TTL);
                //.canonicalLiterals(true);
        pb.parse(StreamRDFLib.graph(m.getGraph()));
        WorkInfo workInfo = new WorkInfo(m, "bdr:W12827_0047");
        om.writerWithDefaultPrettyPrinter().writeValue(System.out, workInfo);
//        ItemInfo itemInfo = new ItemInfo(m, "bdr:I12827");
//        om.writeValue(System.out, itemInfo);
    }
    
    @Test
    public void volumeInfoOutlineTest() throws BDRCAPIException, JsonGenerationException, JsonMappingException, IOException {
        Model m = ModelFactory.createDefaultModel();
        RDFParserBuilder pb = RDFParser.create()
                .source(TESTDIR+"volumeOutline.ttl")
                .lang(RDFLanguages.TTL);
                //.canonicalLiterals(true);
        pb.parse(StreamRDFLib.graph(m.getGraph()));
        VolumeInfo volumeInfo = new VolumeInfo(m, "bdr:V22084_I0890");
        om.writerWithDefaultPrettyPrinter().writeValue(System.out, volumeInfo);
    }
    
    @Test
    public void volumeInfoOutlineFetchTest() throws BDRCAPIException {
        VolumeInfoService.fetchLdsVolumeOutline("bdr:V22084_I0890");
    }
    
    public List<ImageInfo> getTestImageList(String filename) throws JsonParseException, JsonMappingException, IOException {
        final File f = new File(TESTDIR+filename);
        final List<ImageInfo> imageList = om.readValue(f, new TypeReference<List<ImageInfo>>(){});
        return imageList;
    }
    
    @Test
    public void volumeManifestOutlineTest() throws BDRCAPIException, JsonGenerationException, JsonMappingException, IOException {
        Model m = ModelFactory.createDefaultModel();
        RDFParserBuilder pb = RDFParser.create()
                .source(TESTDIR+"volumeOutline.ttl")
                .lang(RDFLanguages.TTL);
                //.canonicalLiterals(true);
        pb.parse(StreamRDFLib.graph(m.getGraph()));
        final VolumeInfo vi = new VolumeInfo(m, "bdr:V22084_I0890");
        final Identifier id = new Identifier("vo:bdr:V22084_I0890", Identifier.MANIFEST_ID);
        final String cacheKey = "W22084/0890";
        final List<ImageInfo> ii = getTestImageList("W22084-0890.json");
        CacheAccess<String, Object> cache = ServiceCache.CACHE;
        cache.put(cacheKey, ii);
        final Manifest mnf = ManifestService.getManifestForIdentifier(id, vi, false);
        //final File fout = new File("/tmp/toto.json"); 
        //IIIFApiObjectMapperProvider.writer.writeValue(fout, mnf);
    }
}
