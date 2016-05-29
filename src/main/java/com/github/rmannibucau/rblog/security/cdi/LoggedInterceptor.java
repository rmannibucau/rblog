package com.github.rmannibucau.rblog.security.cdi;

import com.github.rmannibucau.rblog.security.TokenPrincipal;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;


@Logged
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
public class LoggedInterceptor {
    @Context
    @Inject
    private HttpServletRequest request;

    @AroundInvoke
    public Object userShouldBeLoggedIn(final InvocationContext ic) throws Exception {
        if (!TokenPrincipal.class.isInstance(request.getUserPrincipal())) {
            throw new AccessDeniedException();
        }
        return ic.proceed();
    }
}
