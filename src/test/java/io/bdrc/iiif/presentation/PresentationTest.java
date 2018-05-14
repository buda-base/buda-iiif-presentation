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


public class PresentationTest {

    final static String TESTDIR = "src/test/resources/";
    
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
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(System.out, itemInfo);
    }
      
    
}
