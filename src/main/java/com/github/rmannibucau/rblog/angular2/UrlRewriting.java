package com.github.rmannibucau.rblog.angular2;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

// WARNING: this is a quick workaround for ROOT deployment cause of Angular. If not fixed in Angular will need a js monkey fix.
//
// https://github.com/angular/angular/issues/8498
// note this filter only work for root webapp, for others you would need to hack ROOT one to redirect properly.
@WebFilter(asyncSupported = true, urlPatterns = "/*")
public class UrlRewriting implements Filter {
    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        // no-op
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest httpServletRequest = HttpServletRequest.class.cast(request);
        final String uri = httpServletRequest.getRequestURI().substring(httpServletRequest.getContextPath().length());
        if (isIncluded(uri)) {
            HttpServletResponse.class.cast(response).sendRedirect(httpServletRequest.getContextPath() + "/#" + uri);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isIncluded(final String uri) { // see app.ts
        return uri.startsWith("/login") || uri.startsWith("/logout") ||
                uri.startsWith("/category/") || uri.startsWith("/post/") || uri.startsWith("/search") ||
                uri.startsWith("/admin/");
    }

    @Override
    public void destroy() {
        // no-op
    }
}
