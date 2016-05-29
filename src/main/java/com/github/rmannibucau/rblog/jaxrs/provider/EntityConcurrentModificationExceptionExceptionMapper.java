package com.github.rmannibucau.rblog.jaxrs.provider;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class EntityConcurrentModificationExceptionExceptionMapper implements ExceptionMapper<EntityConcurrentModificationException> {
    @Override
    public Response toResponse(final EntityConcurrentModificationException exception) {
        return Response.status(Response.Status.CONFLICT).build();
    }
}
