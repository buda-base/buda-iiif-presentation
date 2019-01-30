package io.bdrc.iiif.presentation;

import java.io.IOException;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RDFParserBuilder;
import org.apache.jena.riot.system.StreamRDFLib;
import org.apache.jena.system.JenaSystem;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.models.ItemInfo;
import io.bdrc.iiif.presentation.models.VolumeInfo;
import io.bdrc.iiif.presentation.models.WorkInfo;


public class PresentationTest {

    final static String TESTDIR = "src/test/resources/";
    final static ObjectMapper om = new ObjectMapper();
    
    @BeforeClass
    public static void before() {
        JenaSystem.init();
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
    public void volumeOutlineTest() throws BDRCAPIException, JsonGenerationException, JsonMappingException, IOException {
        Model m = ModelFactory.createDefaultModel();
        RDFParserBuilder pb = RDFParser.create()
                .source(TESTDIR+"volumeOutline.ttl")
                .lang(RDFLanguages.TTL);
                //.canonicalLiterals(true);
        pb.parse(StreamRDFLib.graph(m.getGraph()));
        VolumeInfo volumeInfo = new VolumeInfo(m, "bdr:V22084_I0890");
        om.writerWithDefaultPrettyPrinter().writeValue(System.out, volumeInfo);
    }
}
