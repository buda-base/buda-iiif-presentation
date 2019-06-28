package io.bdrc.iiif.presentation;

import static io.bdrc.iiif.presentation.AppConstants.IIIFPresPrefix;
import static io.bdrc.iiif.presentation.AppConstants.IIIFPresPrefix_coll;
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
import io.bdrc.iiif.presentation.models.Identifier;
import io.bdrc.iiif.presentation.models.ImageInfo;
import io.bdrc.iiif.presentation.models.ItemInfo;
import io.bdrc.iiif.presentation.models.PartInfo;
import io.bdrc.iiif.presentation.models.VolumeInfo;
import io.bdrc.iiif.presentation.models.WorkInfo;

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
        ItemInfo itemInfo = new ItemInfo(m, "bdr:I22083");
        om.writeValue(System.out, itemInfo);
    }

    @Test
    public void workInfoModelTest() throws BDRCAPIException, JsonGenerationException, JsonMappingException, IOException {
        Model m = ModelFactory.createDefaultModel();
        RDFParserBuilder pb = RDFParser.create().source(TESTDIR + "workGraphItem.ttl").lang(RDFLanguages.TTL);
        // .canonicalLiterals(true);
        pb.parse(StreamRDFLib.graph(m.getGraph()));
        WorkInfo workInfo = new WorkInfo(m, "bdr:W12827_0047");
        om.writerWithDefaultPrettyPrinter().writeValue(System.out, workInfo);
//        ItemInfo itemInfo = new ItemInfo(m, "bdr:I12827");
//        om.writeValue(System.out, itemInfo);
    }

    @Test
    public void volumeInfoOutlineTest() throws BDRCAPIException, JsonGenerationException, JsonMappingException, IOException {
        Model m = ModelFactory.createDefaultModel();
        RDFParserBuilder pb = RDFParser.create().source(TESTDIR + "volumeOutline.ttl").lang(RDFLanguages.TTL);
        // .canonicalLiterals(true);
        pb.parse(StreamRDFLib.graph(m.getGraph()));
        VolumeInfo volumeInfo = new VolumeInfo(m, "bdr:V22084_I0890");
        om.writerWithDefaultPrettyPrinter().writeValue(System.out, volumeInfo);
    }

    @Test
    public void volumeInfoOutlineFetchTest() throws BDRCAPIException {
        VolumeInfoService.fetchLdsVolumeOutline("bdr:V22084_I0890");
    }

    public List<ImageInfo> getTestImageList(String filename) throws JsonParseException, JsonMappingException, IOException {
        final File f = new File(TESTDIR + filename);
        final List<ImageInfo> imageList = om.readValue(f, new TypeReference<List<ImageInfo>>() {
        });
        return imageList;
    }

    @Test
    public void volumeManifestOutlineTest() throws BDRCAPIException, JsonGenerationException, JsonMappingException, IOException {
        Model m = ModelFactory.createDefaultModel();
        RDFParserBuilder pb = RDFParser.create().source(TESTDIR + "volumeOutline.ttl").lang(RDFLanguages.TTL);
        // .canonicalLiterals(true);
        pb.parse(StreamRDFLib.graph(m.getGraph()));
        final VolumeInfo vi = new VolumeInfo(m, "bdr:V22084_I0890");
        final Identifier id = new Identifier("vo:bdr:V22084_I0890", Identifier.MANIFEST_ID);
        final String cacheKey = "W22084/0890";
        final List<ImageInfo> ii = getTestImageList("W22084-0890.json");
        CacheAccess<String, Object> cache = ServiceCache.CACHE;
        cache.put(cacheKey, ii);
        final Manifest mnf = ManifestService.getManifestForIdentifier(id, vi, false, null, id.getVolumeId(), true);
        //final File fout = new File("/tmp/manifestOutline.json");
        //IIIFApiObjectMapperProvider.writer.writeValue(fout, mnf);
    }

    @Test
    public void volumeManifestWorkVolTest() throws BDRCAPIException, JsonGenerationException, JsonMappingException, IOException {
        Model m = ModelFactory.createDefaultModel();
        RDFParserBuilder pb = RDFParser.create().source(TESTDIR + "volumeOutline.ttl").lang(RDFLanguages.TTL);
        // .canonicalLiterals(true);
        pb.parse(StreamRDFLib.graph(m.getGraph()));
        final VolumeInfo vi = new VolumeInfo(m, "bdr:V22084_I0890");
        // om.writeValue(System.out, vi);
        final Identifier id = new Identifier("wv:bdr:W22084_0002::bdr:V22084_I0890", Identifier.MANIFEST_ID);
        final String cacheKey = "W22084/0890";
        final List<ImageInfo> ii = getTestImageList("W22084-0890.json");
        CacheAccess<String, Object> cache = ServiceCache.CACHE;
        cache.put(cacheKey, ii);
        m = ModelFactory.createDefaultModel();
        pb = RDFParser.create().source(TESTDIR + "workGraphNoItem_location.ttl").lang(RDFLanguages.TTL);
        pb.parse(StreamRDFLib.graph(m.getGraph()));
        final WorkInfo wi = new WorkInfo(m, "bdr:W22084_0002");
        // om.writeValue(System.out, wi);
        final Manifest mnf = ManifestService.getManifestForIdentifier(id, vi, false, wi, id.getVolumeId(), false);
        // final File fout = new File("/tmp/manifestLocation.json");
        // IIIFApiObjectMapperProvider.writer.writeValue(fout, mnf);
    }

    @Test
    public void virtualWork() throws BDRCAPIException, JsonGenerationException, JsonMappingException, IOException {
        Model m = ModelFactory.createDefaultModel();
        RDFParserBuilder pb = RDFParser.create().source(TESTDIR + "workGraphNoItem-virtualwork.ttl").lang(RDFLanguages.TTL);
        // .canonicalLiterals(true);
        pb.parse(StreamRDFLib.graph(m.getGraph()));
        final WorkInfo wi = new WorkInfo(m, "bdr:WSL001_P005");
        final Identifier id = new Identifier("wio:bdr:WSL001_P005", Identifier.COLLECTION_ID);
        final Collection collection = CollectionService.getCommonCollection(id);
        collection.setLabel(CollectionService.getLabels(id.getWorkId(), wi));
        if (wi.parts != null) {
            for (final PartInfo pi : wi.parts) {
                final String collectionId = "wio:" + pi.partId;
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
    public void wioTest() throws BDRCAPIException, JsonGenerationException, JsonMappingException, IOException {
        Model m = ModelFactory.createDefaultModel();
        RDFParserBuilder pb = RDFParser.create().source(TESTDIR + "workGraphNoItem-wio.ttl").lang(RDFLanguages.TTL);
        // .canonicalLiterals(true);
        pb.parse(StreamRDFLib.graph(m.getGraph()));
        final WorkInfo wi = new WorkInfo(m, "bdr:W22073");
        System.out.println(wi.itemId);
        final Identifier id = new Identifier("wio:bdr:W22073", Identifier.COLLECTION_ID);
        final Collection collection = CollectionService.getCommonCollection(id);
        collection.setLabel(CollectionService.getLabels(id.getWorkId(), wi));
        m = ModelFactory.createDefaultModel();
        pb = RDFParser.create().source(TESTDIR + "itemInfoGraph-wio.ttl").lang(RDFLanguages.TTL);
        // .canonicalLiterals(true);
        pb.parse(StreamRDFLib.graph(m.getGraph()));
        // m.write(System.out, "TTL");
        final ItemInfo ii = new ItemInfo(m, "bdr:I22073");
        if (wi.hasLocation) {
            CollectionService.addManifestsForLocation(collection, wi, ii, false);
        } else if (wi.isRoot) {
            final String volPrefix = "v:";
            boolean needsVolumeIndication = ii.volumes.size() > 1;
            for (ItemInfo.VolumeInfoSmall vi : ii.volumes) {
                final String manifestId = volPrefix + vi.getPrefixedUri();
                String manifestUrl;
                if (vi.iiifManifest != null) {
                    manifestUrl = vi.iiifManifest;
                } else {
                    manifestUrl = IIIFPresPrefix + manifestId + "/manifest";
                }
                final Manifest manifest = new Manifest(manifestUrl);
                manifest.setLabel(ManifestService.getLabel(vi.volumeNumber, wi, needsVolumeIndication));
                collection.addManifest(manifest);
            }
        }
//        final File fout = new File("/tmp/wio.json"); 
//        IIIFApiObjectMapperProvider.writer.writeValue(fout, collection);
    }
}
