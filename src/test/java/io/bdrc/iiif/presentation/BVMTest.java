package io.bdrc.iiif.presentation;

import static io.bdrc.iiif.presentation.AppConstants.BDR;
import static io.bdrc.iiif.presentation.AppConstants.CACHEPREFIX_BVM;
import static io.bdrc.iiif.presentation.AppConstants.CACHEPREFIX_IIL;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RDFParserBuilder;
import org.apache.jena.riot.system.StreamRDFLib;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.digitalcollections.iiif.model.sharedcanvas.Manifest;
import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.resmodels.BVM;
import io.bdrc.iiif.presentation.resmodels.ImageGroupInfo;
import io.bdrc.iiif.presentation.resmodels.ImageInfoList;
import io.bdrc.iiif.presentation.resmodels.InstanceInfo;
import io.bdrc.iiif.presentation.resservices.BVMService;
import io.bdrc.iiif.presentation.resservices.ImageInfoListService;
import io.bdrc.iiif.presentation.resservices.ServiceCache;
import io.bdrc.libraries.IdentifierException;

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
        @JsonProperty(value = "display")
        public Boolean display = Boolean.TRUE;
        public String s = "test";

        public TestBooleanDefaultTrue() {
        }
    }

    public static final class TestBooleanDefaultFalse {
        @JsonInclude(Include.NON_DEFAULT)
        @JsonProperty(value = "display")
        public Boolean display = Boolean.FALSE;
        public String s = "test";

        public TestBooleanDefaultFalse() {
        }
    }

    @Test
    public void readComplexBVMTest() throws BDRCAPIException, JsonGenerationException, JsonMappingException, IOException {
        InputStream is = BVMTest.class.getClassLoader().getResourceAsStream("bvmt.json");
        BVM bvm = BVMService.om.readValue(is, BVM.class);
        bvm.validate();
        // ChangeLogItem cli = bvm.getLatestChangeLogItem();
        BVMService.om.writer(BVMService.printer).writeValue(System.out, bvm);
        // testom.writeValue(System.out, new TestBooleanDefaultTrue());
        // testom.writeValue(System.err, new TestBooleanDefaultTrue());
        // testom.writeValue(System.out, new TestBooleanDefaultFalse());
    }

    @BeforeClass
    public static void init() {
        ServiceCache.init();
    }

    @Test
    public void volumeManifestTest() throws BDRCAPIException, JsonGenerationException, JsonMappingException, IOException, IdentifierException {
        InputStream is = BVMTest.class.getClassLoader().getResourceAsStream("bvmt-test1.json");
        BVM bvm = BVMService.om.readValue(is, BVM.class);
        bvm.validate();
        ServiceCache.put(Optional.of(bvm), CACHEPREFIX_BVM + "I0890");
        final String iilCacheKey = CACHEPREFIX_IIL + ImageInfoListService.getKey("W22084", "I0890");
        final ImageInfoList ii = PresentationTest.getTestImageList("dimensions-test1.json");
        ServiceCache.put(Optional.of(ii), iilCacheKey);
        final Identifier id = new Identifier("v:bdr:I0890", Identifier.MANIFEST_ID);
        final ImageGroupInfo vi = new ImageGroupInfo();
        vi.imageGroup = "I0890";
        vi.instanceUri = BDR + "MW22084";
        vi.imageInstanceUri = BDR + "W22084";
        vi.volumeNumber = 1;
        vi.pagesIntroTbrc = 2;
        Model m = ModelFactory.createDefaultModel();
        RDFParserBuilder pb = RDFParser.create().source(TESTDIR + "workOutline.ttl").lang(RDFLanguages.TTL);
        pb.parse(StreamRDFLib.graph(m.getGraph()));
        final InstanceInfo wi = new InstanceInfo(m, "bdr:MW22084");
        Manifest man = ManifestService.getManifestForIdentifier(false, id, vi, false, "bdr:I0890", false, wi);
        final File fout2 = new File("/tmp/bvm-v-manifest.json");
        AppConstants.IIIFMAPPER.writerWithDefaultPrettyPrinter().writeValue(fout2, man);
    }

}
