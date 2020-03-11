package io.bdrc.iiif.presentation;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.resmodels.BVM;
import io.bdrc.iiif.presentation.resmodels.BVM.ChangeLogItem;
import io.bdrc.iiif.presentation.resservices.BVMService;

public class BVMTest {

    final static String TESTDIR = "src/test/resources/";

    @Test
    public void readBVMTest() throws BDRCAPIException, JsonGenerationException, JsonMappingException, IOException {
        InputStream is = BVMTest.class.getClassLoader().getResourceAsStream("bvmt.json");
        BVM bvm = BVMService.om.readValue(is, BVM.class);
        bvm.validate();
        ChangeLogItem cli = bvm.getLatestChangeLogItem();
        //BVMService.om.writer(BVMService.printer).writeValue(System.out, bvm);
    }

}
