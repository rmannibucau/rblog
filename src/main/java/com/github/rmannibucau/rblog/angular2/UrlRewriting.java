package com.github.rmannibucau.rblog.angular2;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

@WebFilter(asyncSupported = true, urlPatterns = "/*")
public class UrlRewriting implements Filter {
    @Override
    public void init(final FilterConfig filterConfig) {
        // no-op
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest httpServletRequest = HttpServletRequest.class.cast(request);
        final String uri = httpServletRequest.getRequestURI().substring(httpServletRequest.getContextPath().length());
        if (isIncluded(uri)) {
            chain.doFilter(new HttpServletRequestWrapper(httpServletRequest) {
                @Override
                public String getServletPath() {
                    return "/index.html";
                }
            }, response);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isIncluded(final String uri) { // see app.routes.ts
        return uri.startsWith("/login") || uri.startsWith("/logout") ||
                uri.startsWith("/category/") || uri.startsWith("/post/") || uri.startsWith("/search") ||
                uri.startsWith("/admin/");
    }

    @Override
    public void destroy() {
        // no-op
    }
}
