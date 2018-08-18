package com.github.rmannibucau.rblog.jaxrs;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.HEAD;
import javax.ws.rs.Path;

import com.github.rmannibucau.rblog.jcache.LocalCacheManager;
import com.github.rmannibucau.rblog.security.cdi.Logged;

@Path("admin")
@ApplicationScoped
public class AdminResource {
    @Inject
    private LocalCacheManager cache;

    @HEAD
    @Logged
    @Path("cache/invalidate")
    public void invalidate() {
        cache.invalidate(null);
    }
}
