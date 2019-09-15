package io.bdrc.iiif.presentation;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Properties;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringRunner;

import com.auth0.client.auth.AuthAPI;
import com.auth0.net.AuthRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.auth.AuthProps;
import io.bdrc.auth.rdf.RdfAuthModel;
import io.bdrc.iiif.presentation.resservices.ServiceCache;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringBootIIIFPres.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class AuthCheck {

	public final static Logger log = LoggerFactory.getLogger(AuthCheck.class.getName());

	static AuthAPI auth;
	static String token;
	static String publicToken;
	static String adminToken;

	@Autowired
	Environment environment;

	@BeforeClass
	public static void init() throws IOException {
		InputStream input = AuthCheck.class.getClassLoader().getResourceAsStream("iiifpres.properties");
		Properties props = new Properties();
		props.load(input);
		InputStream is = new FileInputStream("/etc/buda/share/shared-private.properties");
		props.load(is);
		AuthProps.init(props);
		ServiceCache.init();
		auth = new AuthAPI("bdrc-io.auth0.com", AuthProps.getProperty("lds-pdiClientID"), AuthProps.getProperty("lds-pdiClientSecret"));
		HttpClient client = HttpClientBuilder.create().build();
		HttpPost post = new HttpPost("https://bdrc-io.auth0.com/oauth/token");
		HashMap<String, String> json = new HashMap<>();
		json.put("grant_type", "client_credentials");
		json.put("client_id", AuthProps.getProperty("lds-pdiClientID"));
		json.put("client_secret", AuthProps.getProperty("lds-pdiClientSecret"));
		json.put("audience", "https://bdrc-io.auth0.com/api/v2/");
		ObjectMapper mapper = new ObjectMapper();
		String post_data = mapper.writer().writeValueAsString(json);
		StringEntity se = new StringEntity(post_data);
		se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
		post.setEntity(se);
		HttpResponse response = client.execute(post);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		response.getEntity().writeTo(baos);
		String json_resp = baos.toString();
		baos.close();
		JsonNode node = mapper.readTree(json_resp);
		token = node.findValue("access_token").asText();
		RdfAuthModel.init();
		log.info("USERS >> {}" + RdfAuthModel.getUsers());
		setPublicToken();
		setAdminToken();
	}

	@Test
	public void publicResource() throws ClientProtocolException, IOException, IllegalArgumentException, CertificateException, InvalidKeySpecException, NoSuchAlgorithmException {
		HttpClient client = HttpClientBuilder.create().build();
		HttpGet get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + "/2.1.1/v:bdr:V29329_I1KG15042/manifest");
		HttpResponse resp = client.execute(get);
		log.info("RESP STATUS public resource >> {}", resp.getStatusLine());
		assert (resp.getStatusLine().getStatusCode() == 200);
	}

	@Test
	public void RestrictedResource() throws ClientProtocolException, IOException, IllegalArgumentException, CertificateException, InvalidKeySpecException, NoSuchAlgorithmException {
		// with public Token and authorized picture
		HttpClient client1 = HttpClientBuilder.create().build();
		HttpGet get1 = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + "/2.1.1/v:bdr:V29329_I1KG15042::10-35/manifest");
		get1.addHeader("Authorization", "Bearer " + publicToken);
		HttpResponse resp1 = client1.execute(get1);
		log.info("RESP 1 public Token and authorized picture >> {}", resp1.getStatusLine().getStatusCode());
		assert (resp1.getStatusLine().getStatusCode() == 200);
		// with public Token and restricted picture
		HttpClient client = HttpClientBuilder.create().build();
		HttpGet get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + "/2.1.1/v:bdr:V1KG12713_I1KG12762/manifest");
		get.addHeader("Authorization", "Bearer " + publicToken);
		HttpResponse resp2 = client.execute(get);
		log.info("RESP 2 public Token and authorized picture >> {}", resp2.getStatusLine().getStatusCode());
		assert (resp2.getStatusLine().getStatusCode() == 403);
		// with admin Token and restricted picture = still 403 since the i
		HttpClient client2 = HttpClientBuilder.create().build();
		HttpGet get3 = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + "/2.1.1/v:bdr:V1KG12713_I1KG12762/manifest");
		get3.addHeader("Authorization", "Bearer " + adminToken);
		HttpResponse resp3 = client2.execute(get3);
		log.info("RESP 3 public Token and authorized picture >> {}", resp3.getStatusLine().getStatusCode());
		assert (resp3.getStatusLine().getStatusCode() == 200);
	}

	private static void setPublicToken() throws IOException {
		AuthRequest req = auth.login("publicuser@bdrc.com", AuthProps.getProperty("publicuser@bdrc.com"));
		req.setScope("openid offline_access");
		req.setAudience("https://bdrc-io.auth0.com/api/v2/");
		publicToken = req.execute().getIdToken();
		log.info("public Token >> {}", publicToken);
	}

	private static void setAdminToken() throws IOException {
		AuthRequest req = auth.login("tchame@rimay.net", AuthProps.getProperty("tchame@rimay.net"));
		req.setScope("openid offline_access");
		req.setAudience("https://bdrc-io.auth0.com/api/v2/");
		adminToken = req.execute().getIdToken();
		log.info("admin Token >> {}", adminToken);
	}
}
