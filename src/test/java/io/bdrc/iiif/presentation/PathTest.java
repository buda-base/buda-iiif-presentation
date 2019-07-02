package io.bdrc.iiif.presentation;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;

import io.bdrc.iiif.presentation.exceptions.BDRCAPIExceptionMapper;
import io.bdrc.iiif.presentation.IIIFPresentationService;

public class PathTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(IIIFPresentationService.class).register(BDRCAPIExceptionMapper.class);
    }
    
    @Test
    public void testPathWithVersion() {
        final Response res = target("/2.1.1/v:test/manifest").request().get();
    }
    
    @Test
    public void testPathNoVersion() {
        final Response res = target("/v:test/manifest").request().get();
    }
}
