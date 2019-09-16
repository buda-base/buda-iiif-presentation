package io.bdrc.iiif.presentation;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringRunner;

import io.bdrc.auth.AuthProps;
import io.bdrc.auth.rdf.RdfAuthModel;
import io.bdrc.iiif.presentation.resservices.ServiceCache;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringBootIIIFPres.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class PathTest1 {

	public final static Logger log = LoggerFactory.getLogger(PathTest1.class.getName());

	@Autowired
	Environment environment;

	@BeforeClass
	public static void init() throws IOException {
		InputStream input = AuthCheck.class.getClassLoader().getResourceAsStream("iiifpres.properties");
		Properties props = new Properties();
		props.load(input);
		InputStream is = new FileInputStream("/etc/buda/share/shared-private.properties");
		props.load(is);
		log.info("PROPS >>> {}", props);
		AuthProps.init(props);
		RdfAuthModel.init();
		ServiceCache.init();
	}

	@Test
	public void testPathWithVersion() throws ClientProtocolException, IOException {
		HttpClient client = HttpClientBuilder.create().build();
		HttpGet get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + "/2.1.1/v:bdr:V22334_I3867/manifest");
		get.addHeader("Accept-Language", "eng");
		get.addHeader("Accept-Charset", "UTF-8");
		HttpResponse resp = client.execute(get);
		log.info("RESP STATUS public resource >> {}", resp.getStatusLine());
		assert (resp.getStatusLine().getStatusCode() == 200);
	}

	@Test
	public void testPathNoVersion() throws ClientProtocolException, IOException {
		HttpClient client = HttpClientBuilder.create().build();
		HttpGet get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + "/v:bdr:V22334_I3867/manifest");
		HttpResponse resp = client.execute(get);
		log.info("RESP STATUS public no ver resource >> {}", resp.getStatusLine());
		assert (resp.getStatusLine().getStatusCode() == 200);
	}
}
