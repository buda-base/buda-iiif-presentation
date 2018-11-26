package io.bdrc.iiif.presentation;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Properties;

import javax.ws.rs.core.Application;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.BeforeClass;
import org.junit.Test;

import com.auth0.client.auth.AuthAPI;

import io.bdrc.auth.AuthProps;
import io.bdrc.auth.rdf.RdfAuthModel;

public class AuthCheck extends JerseyTest{

    static AuthAPI auth;
    static String token;
    static String publicToken="eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJodHRwczovL2Rldi1iZHJjLmF1dGgwLmNvbS9hcGkvdjIvIiwic3ViIjoiYXV0aDB8NWJlOTkyZDlkN2VjZTg3ZjE1OWM4YmVkIiwiYXpwIjoiRzBBam1DS3NwTm5nSnNUdFJuSGFBVUNENDRaeHdvTUoiLCJpc3MiOiJodHRwczovL2Rldi1iZHJjLmF1dGgwLmNvbS8iLCJleHAiOjE3MzU3MzgyNjR9.zqOALhi8Gz1io-B1pWIgHVvkSa0U6BuGmB18FnF3CIg\n";
    static String adminToken="eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJodHRwczovL2Rldi1iZHJjLmF1dGgwLmNvbS9hcGkvdjIvIiwic3ViIjoiYXV0aDB8NWJlOTkyMGJlYzMxMjMyMGY1NjI5NGRjIiwiYXpwIjoiRzBBam1DS3NwTm5nSnNUdFJuSGFBVUNENDRaeHdvTUoiLCJpc3MiOiJodHRwczovL2Rldi1iZHJjLmF1dGgwLmNvbS8iLCJleHAiOjE3MzU3Mzc1OTB9.m1V64-90tjNRMD18RQTF8SBlMFOcqgSuPwtALZBLd8U";


    @BeforeClass
    public static void init() throws IOException {
        InputStream input=AuthCheck.class.getClassLoader().getResourceAsStream("iiifpres.properties");
        Properties props=new Properties();
        props.load(input);
        AuthProps.init(props);
        RdfAuthModel.initForStaticTests();
        ServiceCache.init();
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(IIIFPresentationService.class)
                .register(IIIFPresAuthTestFilter.class)
                .register(CommonHeadersFilter.class);

    }

    @Test
    public void publicResource() throws ClientProtocolException, IOException, IllegalArgumentException, CertificateException, InvalidKeySpecException, NoSuchAlgorithmException {
        HttpClient client=HttpClientBuilder.create().build();
        HttpGet get=new HttpGet(this.getBaseUri()+"/2.1.1/v:bdr:V29329_I1KG15042/manifest");
        HttpResponse resp=client.execute(get);
        System.out.println("RESP STATUS >>"+resp.getStatusLine());
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
