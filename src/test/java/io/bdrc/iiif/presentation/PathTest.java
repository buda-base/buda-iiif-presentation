package io.bdrc.iiif.presentation;

import static io.bdrc.iiif.presentation.AppConstants.BDR;
import static io.bdrc.iiif.presentation.AppConstants.CACHEPREFIX_IIL;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.apache.commons.jcs.access.CacheAccess;
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

import io.bdrc.auth.AuthProps;
import io.bdrc.iiif.presentation.resmodels.AccessType;
import io.bdrc.iiif.presentation.resmodels.ImageInfo;
import io.bdrc.iiif.presentation.resmodels.ImageGroupInfo;
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
		final String cacheKey = CACHEPREFIX_IIL+ImageInfoListService.getKey("W22084", "I0890");
        final List<ImageInfo> ii = PresentationTest.getTestImageList("W22084-0890.json");
        CacheAccess<String, Object> cache = ServiceCache.CACHE;
        cache.put(cacheKey, ii);
        final ImageGroupInfo vi = new ImageGroupInfo();
        vi.imageGroup = "I0890";
        vi.workId = BDR+"W22084";
        vi.imageInstanceUri = BDR+"I22084";
        vi.volumeNumber = 1;
        vi.access = AccessType.OPEN;
        vi.restrictedInChina = false;
        vi.statusUri = "http://purl.bdrc.io/admindata/StatusReleased";
        cache.put("vi:bdr:V22084_I0890", vi);
        InputStream input = AuthCheck.class.getClassLoader().getResourceAsStream("iiifpres.properties");
        Properties props = new Properties();
        props.load(input);
        AuthProps.init(props);
	}

	@Test
	public void testPathWithVersion() throws ClientProtocolException, IOException {
		ResponseEntity<String> res = this.restTemplate.getForEntity("http://localhost:" + environment.getProperty("local.server.port") + "/2.1.1/v:bdr:V22084_I0890/manifest",
                String.class);
		//System.out.println(res.getBody());
		assert(res.getStatusCode().equals(HttpStatus.OK));
	}

	@Test
	public void testPathNoVersion() throws ClientProtocolException, IOException {
		ResponseEntity<String> res = this.restTemplate.getForEntity("http://localhost:" + environment.getProperty("local.server.port") + "/v:bdr:V22084_I0890/manifest",
            String.class);
		//System.out.println(res.getBody());
		assert(res.getStatusCode().equals(HttpStatus.OK));
	}
}
