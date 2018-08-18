package com.github.rmannibucau.rblog.jcache;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CacheKeyInvocationContext;
import javax.cache.annotation.GeneratedCacheKey;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.SecurityContext;

@ApplicationScoped
public class JaxRsCacheKeyGenerator implements CacheKeyGenerator {
    @Override
    public GeneratedCacheKey generateCacheKey(final CacheKeyInvocationContext<? extends Annotation> cacheKeyInvocationContext) {
        return new GeneratedCacheKeyImpl(Stream.of(cacheKeyInvocationContext.getKeyParameters())
                .map(this::toValue)
                .toArray());
    }

    private Object toValue(final CacheInvocationParameter cacheInvocationParameter) {
        if (SecurityContext.class.isAssignableFrom(cacheInvocationParameter.getRawType())) {
            final SecurityContext context = SecurityContext.class.cast(cacheInvocationParameter.getValue());
            return context == null || context.getUserPrincipal() == null ? null : context.getUserPrincipal().getName();
        }
        return cacheInvocationParameter.getValue();
    }

    private static class GeneratedCacheKeyImpl implements GeneratedCacheKey {

        private final Object[] params;

        private final int hash;

        private GeneratedCacheKeyImpl(final Object[] parameters) {
            this.params = parameters;
            this.hash = Arrays.deepHashCode(parameters);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final GeneratedCacheKeyImpl that = GeneratedCacheKeyImpl.class.cast(o);
            return Arrays.deepEquals(params, that.params);

        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
