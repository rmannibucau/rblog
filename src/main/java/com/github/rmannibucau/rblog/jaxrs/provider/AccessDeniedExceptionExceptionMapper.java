package com.github.rmannibucau.rblog.jaxrs.provider;

import com.github.rmannibucau.rblog.security.cdi.AccessDeniedException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class AccessDeniedExceptionExceptionMapper implements ExceptionMapper<AccessDeniedException> {
    @Override
    public Response toResponse(final AccessDeniedException exception) {
        return Response.status(Response.Status.FORBIDDEN).build();
    }
}
