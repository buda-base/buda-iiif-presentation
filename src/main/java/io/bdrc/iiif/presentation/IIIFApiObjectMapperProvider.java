package io.bdrc.iiif.presentation;

import javax.inject.Singleton;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import de.digitalcollections.iiif.model.jackson.IiifObjectMapper;

@Singleton
@Provider
public class IIIFApiObjectMapperProvider implements ContextResolver<ObjectMapper> {
    public static final ObjectMapper mapper = new IiifObjectMapper();
    public static final ObjectWriter writer = mapper.writer();
 
    @Override
    public ObjectMapper getContext(Class<?> type) {
        return mapper;
    }
}
