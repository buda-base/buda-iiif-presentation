package io.bdrc.iiif.presentation;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.ClientProtocolException;
import org.apache.jena.rdf.model.Model;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.bdrc.auth.AccessInfo;
import io.bdrc.auth.AccessInfoAuthImpl;
import io.bdrc.auth.AuthProps;
import io.bdrc.auth.TokenValidation;
import io.bdrc.auth.model.Endpoint;
import io.bdrc.auth.rdf.RdfAuthModel;
import io.bdrc.auth.rdf.RdfConstants;
import io.bdrc.auth.rdf.Subscribers;
import io.bdrc.iiif.presentation.GeoLocation;

@RestController
@Component
@RequestMapping("/debug/")
public class TokenController {

    private final ObjectMapper mapper = new ObjectMapper();
    
    @RequestMapping(value = "/debugOtherToken/{token}", method = {RequestMethod.GET, RequestMethod.HEAD})
    public ResponseEntity<String> debugOtherToken(@PathVariable("token") String token, HttpServletRequest request, HttpServletResponse response,
            WebRequest webRequest)
            throws ClientProtocolException, IOException {
        return debugToken(request, response, webRequest, Optional.of(token));
    }
    
    @RequestMapping(value = "/debugToken", method = {RequestMethod.GET, RequestMethod.HEAD})
    public ResponseEntity<String> debugSelfToken(HttpServletRequest request, HttpServletResponse response,
            WebRequest webRequest)
            throws ClientProtocolException, IOException {
        return debugToken(request, response, webRequest, Optional.empty());
    }
    
    public static String modelToTtl(final Model m) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        m.write(baos, "TTL");
        return new String(baos.toByteArray(), StandardCharsets.UTF_8);
    }
    
    @RequestMapping(value = "/debugAuthModel", method = {RequestMethod.GET, RequestMethod.HEAD})
    public ResponseEntity<String> debugAuthModel(HttpServletRequest request, HttpServletResponse response,
            WebRequest webRequest)
            throws ClientProtocolException, IOException {
        final HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        final ObjectNode rootNode = mapper.createObjectNode();
        rootNode.set("subscriberToCollections", mapper.valueToTree(Subscribers.subscriberToCollections));
        rootNode.set("collectionToSubscribers", mapper.valueToTree(Subscribers.collectionToSubscribers));
        rootNode.put("authModelTtl", modelToTtl(RdfAuthModel.getFullModel()));
    	return new ResponseEntity<String>(mapper.writeValueAsString(rootNode),
                headers, HttpStatus.OK);
    }
        
    public ResponseEntity<String> debugToken(HttpServletRequest request, HttpServletResponse response,
                WebRequest webRequest, Optional<String> token)
                throws ClientProtocolException, IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        final ObjectNode rootNode = mapper.createObjectNode();
        
        AccessInfoAuthImpl acc = null;
        
        if (!token.isPresent()) {
            // debugging the token of the request
        
            String mytoken = IIIFPresAuthFilter.getToken(((HttpServletRequest) request).getHeader("Authorization"));
            if (mytoken == null) {
                Cookie[] cookies = ((HttpServletRequest) request).getCookies();
                if (cookies != null) {
                    for (Cookie cook : cookies) {
                        if (cook.getName().equals(AuthProps.getProperty("cookieKey"))) {
                            mytoken = cook.getValue();
                            break;
                        }
                    }
                }
            }
            rootNode.put("token", mytoken);
            acc = (AccessInfoAuthImpl) request.getAttribute("access");
            if (acc == null || !acc.isLogged())
                return new ResponseEntity<String>("\"You must be authenticated to access this service\"",
                        headers, HttpStatus.UNAUTHORIZED);
        } else {
            // debugging a token passed as argument
            rootNode.put("token", token.get());
            TokenValidation validation = new TokenValidation(token.get());
            rootNode.put("tokenvalid", validation.isValid());
            acc = new AccessInfoAuthImpl(validation.getUser(), new Endpoint());
            if (acc == null || !acc.isLogged())
                return new ResponseEntity<String>("\"a valid access cannot be built from this token\"",
                        headers, HttpStatus.UNAUTHORIZED);
            
        }
        
        rootNode.put("userprofile", acc.getUserProfile().toString());
        rootNode.put("user", acc.getUser().toString());
        List<String> personalAccessL = RdfAuthModel.getPersonalAccess(RdfConstants.AUTH_RESOURCE_BASE + acc.getUser().getUserId());
        ArrayNode an = mapper.valueToTree(personalAccessL);
        rootNode.putArray("personalAccess").addAll(an);
        final String ipAddress = request.getHeader(GeoLocation.HEADER_NAME);
        rootNode.put("ip", ipAddress);
        rootNode.put("subscriber", Subscribers.getCachedSubscriber(ipAddress));
        String test = GeoLocation.getCountryCode(ipAddress);
        rootNode.put("inChina", test == null || "CN".equalsIgnoreCase(test));
        rootNode.put("isAdmin", acc.getUserProfile().isAdmin());
        
        return new ResponseEntity<String>(mapper.writeValueAsString(rootNode),
                headers, HttpStatus.OK);
        
    }
    
}
