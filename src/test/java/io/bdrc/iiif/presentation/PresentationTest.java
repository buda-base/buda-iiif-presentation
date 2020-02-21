package io.bdrc.iiif.presentation;

import static io.bdrc.iiif.presentation.AppConstants.IIIFPresPrefix;
import static io.bdrc.iiif.presentation.AppConstants.IIIFPresPrefix_coll;
import static io.bdrc.iiif.presentation.AppConstants.CACHEPREFIX_IIL;
import static io.bdrc.iiif.presentation.AppConstants.BDR;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

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

import de.digitalcollections.iiif.model.PropertyValue;
import de.digitalcollections.iiif.model.sharedcanvas.Collection;
import de.digitalcollections.iiif.model.sharedcanvas.Manifest;
import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.resmodels.ImageInfo;
import io.bdrc.iiif.presentation.resmodels.ImageInstanceInfo;
import io.bdrc.iiif.presentation.resmodels.PartInfo;
import io.bdrc.iiif.presentation.resmodels.ImageGroupInfo;
import io.bdrc.iiif.presentation.resmodels.InstanceInfo;
import io.bdrc.iiif.presentation.resmodels.InstanceOutline;
import io.bdrc.iiif.presentation.resservices.ImageInfoListService;
import io.bdrc.iiif.presentation.resservices.ServiceCache;
import io.bdrc.iiif.presentation.resservices.ImageGroupInfoService;
import io.bdrc.libraries.Identifier;
import io.bdrc.libraries.IdentifierException;

public class PresentationTest {

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
        Model m = ModelFactory.createDefaultModel();
        RDFParserBuilder pb = RDFParser.create().source(TESTDIR + "itemInfoGraph.ttl").lang(RDFLanguages.TTL);
        // .canonicalLiterals(true);
        pb.parse(StreamRDFLib.graph(m.getGraph()));
        ImageInstanceInfo itemInfo = new ImageInstanceInfo(m, "bdr:W22083");
        om.writeValue(System.out, itemInfo);
    }

    @Test
    public void workInfoModelTest() throws BDRCAPIException, JsonGenerationException, JsonMappingException, IOException {
        Model m = ModelFactory.createDefaultModel();
        RDFParserBuilder pb = RDFParser.create().source(TESTDIR + "workGraphItem.ttl").lang(RDFLanguages.TTL);
        // .canonicalLiterals(true);
        pb.parse(StreamRDFLib.graph(m.getGraph()));
        InstanceInfo workInfo = new InstanceInfo(m, "bdr:MW12827_0047");
        om.writerWithDefaultPrettyPrinter().writeValue(System.out, workInfo);
//        ItemInfo itemInfo = new ItemInfo(m, "bdr:I12827");
//        om.writeValue(System.out, itemInfo);
    }

    public static List<ImageInfo> getTestImageList(String filename) throws JsonParseException, JsonMappingException, IOException {
        final File f = new File(TESTDIR + filename);
        final List<ImageInfo> imageList = om.readValue(f, new TypeReference<List<ImageInfo>>() {});
        imageList.removeIf(imageInfo -> imageInfo.filename.endsWith("json"));
        return imageList;
    }

    @Test
    public void virtualWork() throws BDRCAPIException, JsonGenerationException, JsonMappingException, IOException, IdentifierException {
        Model m = ModelFactory.createDefaultModel();
        RDFParserBuilder pb = RDFParser.create().source(TESTDIR + "workGraphNoItem-virtualwork.ttl").lang(RDFLanguages.TTL);
        pb.parse(StreamRDFLib.graph(m.getGraph()));
        final InstanceInfo wi = new InstanceInfo(m, "bdr:MWSL001_P005");
        // om.writeValue(System.out, wi);
        final Identifier id = new Identifier("wio:bdr:MWSL001_P005", Identifier.COLLECTION_ID);
        final Collection collection = CollectionService.getCommonCollection(id);
        collection.setLabel(CollectionService.getLabels(id.getInstanceId(), wi.labels));
        if (wi.parts != null) {
            for (final PartInfo pi : wi.parts) {
                final String collectionId = "wio:" + pi.partQname;
                final Collection subcollection = new Collection(IIIFPresPrefix_coll + collectionId);
                final PropertyValue labels = ManifestService.getPropForLabels(pi.labels);
                subcollection.setLabel(labels);
                collection.addCollection(subcollection);
            }
        }
        // final File fout = new File("/tmp/virtualWork.json");
        // IIIFApiObjectMapperProvider.writer.writeValue(fout, collection);
    }

    @Test
    public void virtualWorkPart() throws BDRCAPIException, JsonGenerationException, JsonMappingException, IOException, IdentifierException {
        Model m = ModelFactory.createDefaultModel();
        RDFParserBuilder pb = RDFParser.create().source(TESTDIR + "workGraphNoItem-virtualworkpart.ttl").lang(RDFLanguages.TTL);
        pb.parse(StreamRDFLib.graph(m.getGraph()));
        final InstanceInfo wi = new InstanceInfo(m, "bdr:MWSL001");
        // om.writeValue(System.out, wi);
        final Identifier id = new Identifier("wio:bdr:MWSL001", Identifier.COLLECTION_ID);
        //final Collection collection = CollectionService.getCollectionForOutline(CollectionService.getCommonCollection(id), id, wi, false);
        //final File fout = new File("/tmp/virtualWorkpart.json");
        //IIIFApiObjectMapperProvider.writer.writeValue(fout, collection);
    }
    
    @Test
    public void virtualWorkLinktoPart() throws BDRCAPIException, JsonGenerationException, JsonMappingException, IOException, IdentifierException {
        Model m = ModelFactory.createDefaultModel();
        RDFParserBuilder pb = RDFParser.create().source(TESTDIR + "workGraphNoItem-virtualworklinktopart.ttl").lang(RDFLanguages.TTL);
        pb.parse(StreamRDFLib.graph(m.getGraph()));
        final InstanceInfo wi = new InstanceInfo(m, "bdr:MW1ERI0009001_01_02_02");
        //om.writeValue(System.out, wi);
        final Identifier id = new Identifier("wio:bdr:MW1ERI0009001_01_02_02", Identifier.COLLECTION_ID);
        //final Collection collection = CollectionService.getCollectionForOutline(CollectionService.getCommonCollection(id), id, wi, false);
        //final File fout = new File("/tmp/virtualWorkLinkToPart.json");
        //IIIFApiObjectMapperProvider.writer.writeValue(fout, collection);
    }

    @Test
    public void virtualWorkLocation() throws BDRCAPIException, JsonGenerationException, JsonMappingException, IOException, IdentifierException {
        Model m = ModelFactory.createDefaultModel();
        RDFParserBuilder pb = RDFParser.create().source(TESTDIR + "workGraphNoItem-virtualworklocation.ttl").lang(RDFLanguages.TTL);
        pb.parse(StreamRDFLib.graph(m.getGraph()));
        final InstanceInfo wi = new InstanceInfo(m, "bdr:MW1ERI0009001_01_01_01");
        //om.writeValue(System.out, wi);
        final Identifier id = new Identifier("wio:bdr:MW1ERI0009001_01_01_01", Identifier.COLLECTION_ID);
        //final Collection collection = CollectionService.getCollectionForOutline(CollectionService.getCommonCollection(id), id, wi, false);
        //final File fout = new File("/tmp/virtualWorkLocation.json");
        //IIIFApiObjectMapperProvider.writer.writeValue(fout, collection);
    }
    
    @Test
    public void workOutlineBasics() throws BDRCAPIException, JsonGenerationException, JsonMappingException, IOException, IdentifierException {
        Model m = ModelFactory.createDefaultModel();
        RDFParserBuilder pb = RDFParser.create().source(TESTDIR + "workOutline.ttl").lang(RDFLanguages.TTL);
        pb.parse(StreamRDFLib.graph(m.getGraph()));
        final InstanceOutline wo = new InstanceOutline(m, "bdr:MW22084");
        //final File fout = new File("/tmp/workOutline.json");
        //om.writeValue(fout, wo);
        final Identifier id = new Identifier("wvo:bdr:MW22084::bdr:I0890", Identifier.MANIFEST_ID);
        final String cacheKey = CACHEPREFIX_IIL+ImageInfoListService.getKey("W22084", "I0890");
        final List<ImageInfo> ii = getTestImageList("W22084-0890.json");
        
        CacheAccess<String, Object> cache = ServiceCache.CACHE;
        cache.put(cacheKey, ii);
        final ImageGroupInfo vi = new ImageGroupInfo();
        vi.imageGroup = "I0890";
        vi.instanceUri = BDR+"MW22084";
        vi.imageInstanceUri = BDR+"W22084";
        vi.volumeNumber = 1;
        Manifest man = ManifestService.getManifestForIdentifier(id, vi, false, "bdr:I0890", false, wo.getPartForInstanceId("bdr:MW22084"));
        //final File fout2 = new File("/tmp/workOutline-manifest.json");
        //IIIFApiObjectMapperProvider.writer.writeValue(fout2, man);
    }

    @Test
    public void wvBasics() throws BDRCAPIException, JsonGenerationException, JsonMappingException, IOException, IdentifierException {
        Model m = ModelFactory.createDefaultModel();
        RDFParserBuilder pb = RDFParser.create().source(TESTDIR + "workOutline.ttl").lang(RDFLanguages.TTL);
        pb.parse(StreamRDFLib.graph(m.getGraph()));
        final InstanceInfo wi = new InstanceInfo(m, "bdr:MW22084_01_01");
        //System.out.println(wi.toString());
        //final File fout = new File("/tmp/wv.json");
        //om.writeValue(fout, wi);
        final Identifier id = new Identifier("wv:bdr:MW22084_01_01::bdr:I0890", Identifier.MANIFEST_ID);
        final String cacheKey = CACHEPREFIX_IIL+ImageInfoListService.getKey("W22084", "I0890");
        final List<ImageInfo> ii = getTestImageList("W22084-0890.json");
        CacheAccess<String, Object> cache = ServiceCache.CACHE;
        cache.put(cacheKey, ii);
        final ImageGroupInfo vi = new ImageGroupInfo();
        vi.imageGroup = "I0890";
        vi.instanceUri = BDR+"MW22084";
        vi.imageInstanceUri = BDR+"W22084";
        vi.volumeNumber = 1;
        Manifest man = ManifestService.getManifestForIdentifier(id, vi, false, "bdr:I0890", false, wi);
        //final File fout2 = new File("/tmp/wv-manifest.json");
        //AppConstants.IIIFMAPPER.writer().writeValue(fout2, man);
    }
    
    @Test
    public void wioTest() throws BDRCAPIException, JsonGenerationException, JsonMappingException, IOException, IdentifierException {
        Model m = ModelFactory.createDefaultModel();
        RDFParserBuilder pb = RDFParser.create().source(TESTDIR + "workGraphNoItem-wio.ttl").lang(RDFLanguages.TTL);
        // .canonicalLiterals(true);
        pb.parse(StreamRDFLib.graph(m.getGraph()));
        final InstanceInfo wi = new InstanceInfo(m, "bdr:MW22073");
        // System.out.println(wi.itemId);
        final Identifier id = new Identifier("wio:bdr:MW22073", Identifier.COLLECTION_ID);
        final Collection collection = CollectionService.getCommonCollection(id);
        collection.setLabel(CollectionService.getLabels(id.getInstanceId(), wi.labels));
        m = ModelFactory.createDefaultModel();
        pb = RDFParser.create().source(TESTDIR + "itemInfoGraph-wio.ttl").lang(RDFLanguages.TTL);
        // .canonicalLiterals(true);
        pb.parse(StreamRDFLib.graph(m.getGraph()));
        // m.write(System.out, "TTL");
        final ImageInstanceInfo ii = new ImageInstanceInfo(m, "bdr:W22073");
        if (wi.hasLocation) {
            CollectionService.addManifestsForLocation(collection, wi, ii, false);
        } else if (wi.isRoot) {
            final String volPrefix = "v:";
            boolean needsVolumeIndication = ii.volumes.size() > 1;
            for (ImageInstanceInfo.VolumeInfoSmall vi : ii.volumes) {
                final String manifestId = volPrefix + vi.getPrefixedUri();
                String manifestUrl;
                if (vi.iiifManifestUri != null) {
                    manifestUrl = vi.iiifManifestUri;
                } else {
                    manifestUrl = IIIFPresPrefix + manifestId + "/manifest";
                }
                final Manifest manifest = new Manifest(manifestUrl);
                manifest.setLabel(ManifestService.getLabel(vi.volumeNumber, wi.labels, needsVolumeIndication));
                collection.addManifest(manifest);
            }
        }
//        final File fout = new File("/tmp/wio.json"); 
//        IIIFApiObjectMapperProvider.writer.writeValue(fout, collection);
    }
}
