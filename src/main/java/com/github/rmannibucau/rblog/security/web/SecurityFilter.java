package com.github.rmannibucau.rblog.security.web;

import com.github.rmannibucau.rblog.jpa.Token;
import com.github.rmannibucau.rblog.security.TokenPrincipal;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import javax.transaction.TransactionalException;
import java.io.IOException;
import java.security.Principal;
import java.util.Date;

import static java.util.Optional.ofNullable;

@WebFilter(urlPatterns = "/*", asyncSupported = true)
public class SecurityFilter implements Filter {
    public static final String SECURITY_HEADER = "RBLOG-SECURITY-TOKEN";

    @Inject
    private TokenUpdater tokenUpdater;

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        if (!HttpServletRequest.class.isInstance(request)) {
            chain.doFilter(request, response);
            return;
        }

        final HttpServletRequest httpServletRequest = HttpServletRequest.class.cast(request);

        final String token = httpServletRequest.getHeader(SECURITY_HEADER);
        if (token == null) {
            chain.doFilter(request, response);
            return;
        }

        try {
            final Token loadedToken = tokenUpdater.findAndUpdate(token);
            final TokenPrincipal tokenPrincipal = new TokenPrincipal(loadedToken.getUser().getUsername(), loadedToken.getUser().getDisplayName(), loadedToken.getValue());

            chain.doFilter(new HttpServletRequestWrapper(httpServletRequest) {
                @Override
                public Principal getUserPrincipal() {
                    return tokenPrincipal;
                }
            }, response);
        } catch (final TransactionalException iae) {
            if (IllegalArgumentException.class.isInstance(iae.getCause())) { // Invalid token
                HttpServletResponse.class.cast(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                throw iae;
            }
        }
    }

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        // no-op
    }

    @Override
    public void destroy() {
        // no-op
    }

    @ApplicationScoped
    public static class TokenUpdater {
        @Inject
        private EntityManager entityManager;

        @Transactional // in its own tx cause if then the business one fails we still want to update the token timestamp
        public Token findAndUpdate(final String token) {
            return ofNullable(entityManager.find(Token.class, token))
                .map(t -> {
                    t.setLastUsage(new Date());
                    return t;
                })
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));
        }
    }
}
