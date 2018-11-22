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

import javax.ws.rs.core.Application;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.BeforeClass;
import org.junit.Test;

import com.auth0.client.auth.AuthAPI;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.auth.AuthProps;
import io.bdrc.auth.rdf.RdfAuthModel;

public class AuthTest extends JerseyTest{

    static AuthAPI auth;
    static String token;
    static String publicToken;
    static String adminToken;

    @BeforeClass
    public static void init() throws IOException {

        InputStream is=new FileInputStream("/etc/buda/iiifserv/iiifservTest.properties");
        Properties props=new Properties();
        props.load(is);
        AuthProps.init(props);
        auth = new AuthAPI(AuthProps.getProperty("authAPI"), AuthProps.getProperty("lds-pdiClientID"), AuthProps.getProperty("lds-pdiClientSecret"));
        HttpClient client=HttpClientBuilder.create().build();
        HttpPost post=new HttpPost(AuthProps.getProperty("issuer")+"oauth/token");
        HashMap<String,String> json = new HashMap<>();
        json.put("grant_type","password");
        json.put("username","admin@bdrc-test.com");
        json.put("password",AuthProps.getProperty("admin@bdrc-test.com"));
        json.put("client_id",AuthProps.getProperty("lds-pdiClientID"));
        json.put("client_secret",AuthProps.getProperty("lds-pdiClientSecret"));
        json.put("audience",AuthProps.getProperty("audience"));
        ObjectMapper mapper=new ObjectMapper();
        String post_data=mapper.writer().writeValueAsString(json);
        StringEntity se = new StringEntity(post_data);
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        post.setEntity(se);
        HttpResponse response = client.execute(post);
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        response.getEntity().writeTo(baos);
        String json_resp=baos.toString();
        baos.close();
        JsonNode node=mapper.readTree(json_resp);
        token=node.findValue("access_token").asText();
        RdfAuthModel.initForTest(false,true);
        ServiceCache.init();
        setTokens();
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(IIIFPresentationService.class)
                .register(IIIFPresAuthFilter.class);

    }

    private static void setTokens() throws IOException {
        adminToken=getToken("admin@bdrc-test.com");
        publicToken=getToken("public@bdrc-test.com");
    }

    private static String getToken(String username) throws IOException {
        String tok="";
        HttpClient client=HttpClientBuilder.create().build();
        HttpPost post=new HttpPost(AuthProps.getProperty("issuer")+"oauth/token");
        HashMap<String,String> json = new HashMap<>();
        json.put("grant_type","password");
        json.put("username",username);
        json.put("password",AuthProps.getProperty(username));
        json.put("client_id",AuthProps.getProperty("lds-pdiClientID"));
        json.put("client_secret",AuthProps.getProperty("lds-pdiClientSecret"));
        json.put("audience",AuthProps.getProperty("audience"));
        ObjectMapper mapper=new ObjectMapper();
        String post_data=mapper.writer().writeValueAsString(json);
        StringEntity se = new StringEntity(post_data);
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        post.setEntity(se);
        HttpResponse response = client.execute(post);
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        response.getEntity().writeTo(baos);
        String json_resp=baos.toString();
        baos.close();
        JsonNode node=mapper.readTree(json_resp);
        tok=node.findValue("access_token").asText();
        return tok;
    }

    @Test
    public void publicResource() throws ClientProtocolException, IOException, IllegalArgumentException, CertificateException, InvalidKeySpecException, NoSuchAlgorithmException {
        HttpClient client=HttpClientBuilder.create().build();
        HttpGet get=new HttpGet(this.getBaseUri()+"/2.1.1/v:bdr:V29329_I1KG15042::10-35/manifest");
        HttpResponse resp=client.execute(get);
        assert(resp.getStatusLine().getStatusCode()==200);
    }

    @Test
    public void ChinaRestrictedResource() throws ClientProtocolException, IOException, IllegalArgumentException, CertificateException, InvalidKeySpecException, NoSuchAlgorithmException {
        //with public Token and authorized picture
        HttpClient client1=HttpClientBuilder.create().build();
        HttpGet get1=new HttpGet(this.getBaseUri()+"2.1.1/v:bdr:V29329_I1KG15042::10-35/manifest");
        get1.addHeader("Authorization", "Bearer "+publicToken);
        HttpResponse resp1=client1.execute(get1);
        System.out.println("RESP 1 >>"+resp1.getStatusLine());
        assert(resp1.getStatusLine().getStatusCode()==200);
        //with public Token and restricted picture
        HttpClient client=HttpClientBuilder.create().build();
        HttpGet get=new HttpGet(this.getBaseUri()+"2.1.1/v:bdr:V28263_I1KG14453/manifest");
        get.addHeader("Authorization", "Bearer "+publicToken);
        HttpResponse resp=client.execute(get);
        System.out.println("RESP 2 >>"+resp.getStatusLine());
        assert(resp.getStatusLine().getStatusCode()==403);
        //with admin Token and restricted picture
        HttpClient client2=HttpClientBuilder.create().build();
        HttpGet get2=new HttpGet(this.getBaseUri()+"2.1.1/v:bdr:V28263_I1KG14453/manifest");
        get2.addHeader("Authorization", "Bearer "+adminToken);
        HttpResponse resp2=client2.execute(get2);
        System.out.println("RESP 3 >>"+resp2.getStatusLine());
        assert(resp2.getStatusLine().getStatusCode()==200);
    }
}
