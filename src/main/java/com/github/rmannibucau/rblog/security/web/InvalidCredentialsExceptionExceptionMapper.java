package com.github.rmannibucau.rblog.security.web;

import com.github.rmannibucau.rblog.security.web.model.TokenError;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class InvalidCredentialsExceptionExceptionMapper implements ExceptionMapper<InvalidCredentialsException> {
    private final TokenError error = new TokenError("bad credentials");

    public Response toResponse(final InvalidCredentialsException exception) {
        return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
    }
}
