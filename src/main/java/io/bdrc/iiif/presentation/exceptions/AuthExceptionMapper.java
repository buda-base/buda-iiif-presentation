package io.bdrc.iiif.presentation.exceptions;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class AuthExceptionMapper implements ExceptionMapper<AuthException> {

    @Override
    public Response toResponse(AuthException exception) 
    {
        ResponseBuilder rs = Response.status(exception.status);
        rs = rs.entity(new ErrorMessage(exception)).type(MediaType.APPLICATION_JSON);
        return rs.build();
    }
    
}
