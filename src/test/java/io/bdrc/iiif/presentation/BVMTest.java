package io.bdrc.iiif.presentation;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.resmodels.BVM;
import io.bdrc.iiif.presentation.resmodels.BVM.ChangeLogItem;
import io.bdrc.iiif.presentation.resservices.BVMService;

public class BVMTest {

    final static String TESTDIR = "src/test/resources/";
    
    public final static ObjectMapper testom = new ObjectMapper();
    
    public static final class TrueFilter {

        @Override
        public boolean equals(final Object obj) {
            if (obj == null || !(obj instanceof Boolean)) {
                return false;
            }
            final Boolean v = (Boolean) obj;
            return Boolean.TRUE.equals(v);
        }
    }
    
    public static final class TestBooleanDefaultTrue {
        @JsonInclude(value = Include.CUSTOM, valueFilter = TrueFilter.class)
        @JsonProperty(value="display")
        public Boolean display = Boolean.TRUE;
        public String s = "test";
        
        public TestBooleanDefaultTrue() {}
    }

    public static final class TestBooleanDefaultFalse {
        @JsonInclude(Include.NON_DEFAULT)
        @JsonProperty(value="display")
        public Boolean display = Boolean.FALSE;
        public String s = "test";
        
        public TestBooleanDefaultFalse() {}
    }
    
    @Test
    public void readBVMTest() throws BDRCAPIException, JsonGenerationException, JsonMappingException, IOException {
        InputStream is = BVMTest.class.getClassLoader().getResourceAsStream("bvmt.json");
        BVM bvm = BVMService.om.readValue(is, BVM.class);
        bvm.validate();
        ChangeLogItem cli = bvm.getLatestChangeLogItem();
        BVMService.om.writer(BVMService.printer).writeValue(System.out, bvm);
        //testom.writeValue(System.out, new TestBooleanDefaultTrue());
        //testom.writeValue(System.err, new TestBooleanDefaultTrue());
        //testom.writeValue(System.out, new TestBooleanDefaultFalse());
    }

}
