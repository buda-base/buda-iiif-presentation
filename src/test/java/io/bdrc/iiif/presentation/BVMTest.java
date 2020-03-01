package io.bdrc.iiif.presentation;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.resservices.ServiceCache;

public class BVMTest {

    final static String TESTDIR = "src/test/resources/";
    final static ObjectMapper om = new ObjectMapper();

    @BeforeClass
    public static void before() {
        ServiceCache.init();
    }
    
    @Test
    public void accessHeaderTest() {
        assertTrue("123".equals(IIIFPresAuthFilter.getToken("Bearer 123")));
    }

    @Test
    public void itemInfoModelTest() throws BDRCAPIException, JsonGenerationException, JsonMappingException, IOException {
        
    }

}
