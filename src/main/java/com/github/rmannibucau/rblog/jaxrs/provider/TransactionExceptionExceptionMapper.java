package com.github.rmannibucau.rblog.jaxrs.provider;

import lombok.extern.java.Log;

import javax.transaction.TransactionalException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;
import java.util.logging.Level;

@Log
@Provider
public class TransactionExceptionExceptionMapper implements ExceptionMapper<TransactionalException> {
    @Context
    private Providers providers;

    @Override
    public Response toResponse(final TransactionalException exception) {
        final Throwable cause = exception.getCause();
        if (cause != null) {
            final Class causeType = cause.getClass();
            final ExceptionMapper exceptionMapper = this.providers.getExceptionMapper(causeType);
            if (exceptionMapper == null) { // let it be a HTTP 500
                log.log(Level.SEVERE, cause.getMessage(), cause);
            } else {
                return exceptionMapper.toResponse(cause);
            }
        }
        return Response.serverError().build();
    }
}
