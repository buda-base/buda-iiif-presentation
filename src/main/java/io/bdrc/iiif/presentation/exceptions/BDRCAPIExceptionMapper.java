package io.bdrc.iiif.presentation.exceptions;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class BDRCAPIExceptionMapper implements ExceptionMapper<BDRCAPIException> {

    @Override
    public Response toResponse(BDRCAPIException exception) 
    {
        return Response.status(exception.status)
                .entity(new ErrorMessage(exception))
                .header("Access-Control-Allow-Origin", "*")
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
    
}
