package io.bdrc.iiif.presentation;

import javax.inject.Singleton;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.digitalcollections.iiif.model.jackson.IiifObjectMapper;

@Singleton
@Provider
public class IIIFApiObjectMapperProvider implements ContextResolver<ObjectMapper> {
    private ObjectMapper mapper = null;
 
    public IIIFApiObjectMapperProvider() {
        super();
        mapper = new IiifObjectMapper();
    }
 
    @Override
    public ObjectMapper getContext(Class<?> type) {
        return mapper;
    }
}
