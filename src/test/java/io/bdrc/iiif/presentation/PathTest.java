package io.bdrc.iiif.presentation;

import static io.bdrc.iiif.presentation.AppConstants.BDR;
import static io.bdrc.iiif.presentation.AppConstants.CACHEPREFIX_BVM;
import static io.bdrc.iiif.presentation.AppConstants.CACHEPREFIX_IIL;
import static io.bdrc.iiif.presentation.AppConstants.CACHEPREFIX_VI;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

import org.apache.http.client.ClientProtocolException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.bdrc.auth.AuthProps;
import io.bdrc.iiif.presentation.resmodels.AccessType;
import io.bdrc.iiif.presentation.resmodels.ImageGroupInfo;
import io.bdrc.iiif.presentation.resmodels.ImageInfoList;
import io.bdrc.iiif.presentation.resservices.ImageGroupInfoService;
import io.bdrc.iiif.presentation.resservices.ImageInfoListService;
import io.bdrc.iiif.presentation.resservices.ServiceCache;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringBootIIIFPres.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("local")
public class PathTest {

    public final static Logger log = LoggerFactory.getLogger(PathTest.class.getName());

    @Autowired
    Environment environment;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeClass
    public static void init() throws IOException {
        ServiceCache.init();
        InputStream input = AuthCheck.class.getClassLoader().getResourceAsStream("iiifpres.properties");
        Properties props = new Properties();
        props.load(input);
        AuthProps.init(props);
    }
    
    public static void initCache() throws JsonParseException, JsonMappingException, IOException {
        final String cacheKey = CACHEPREFIX_IIL + ImageInfoListService.getKey("W22084", "I0890");
        final ImageInfoList ii = PresentationTest.getTestImageList("W22084-0890.json");
        ServiceCache.put(Optional.of(ii), cacheKey);
        final ImageGroupInfo vi = new ImageGroupInfo();
        vi.imageGroupLname = "I0890";
        vi.instanceUri = BDR + "MW22084";
        vi.imageInstanceUri = BDR + "W22084";
        vi.volumeNumber = 1;
        vi.access = AccessType.OPEN;
        vi.restrictedInChina = false;
        vi.statusUri = "http://purl.bdrc.io/admindata/StatusReleased";
        ServiceCache.put(Optional.of(vi), CACHEPREFIX_VI+"bdr:I0890");
        ServiceCache.put(Optional.empty(), CACHEPREFIX_BVM + "I0890");
    }

    @Test
    public void testPathWithVersion() throws ClientProtocolException, IOException {
        initCache();
        ResponseEntity<String> res = this.restTemplate
                .getForEntity("http://localhost:" + environment.getProperty("local.server.port") + "/2.1.1/v:bdr:I0890/manifest", String.class);
        // System.out.println(res.getBody());
        assert (res.getStatusCode().equals(HttpStatus.OK));
    }

    @Test
    public void testPathNoVersion() throws ClientProtocolException, IOException {
        initCache();
        ResponseEntity<String> res = this.restTemplate
                .getForEntity("http://localhost:" + environment.getProperty("local.server.port") + "/v:bdr:I0890/manifest", String.class);
        // System.out.println(res.getBody());
        assert (res.getStatusCode().equals(HttpStatus.OK));
    }
}
