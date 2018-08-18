package com.github.rmannibucau.rblog.jcache;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheResolver;
import javax.cache.annotation.CacheResolverFactory;
import javax.cache.annotation.CacheResult;
import javax.cache.configuration.Configuration;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.Duration;
import javax.cache.expiry.EternalExpiryPolicy;
import javax.cache.expiry.TouchedExpiryPolicy;
import javax.cache.spi.CachingProvider;
import javax.enterprise.context.ApplicationScoped;

import org.apache.geronimo.jcache.simple.cdi.CacheResolverImpl;

@ApplicationScoped
public class LocalCacheManager implements CacheResolverFactory {
    private CacheManager cacheManager;
    private CachingProvider provider;

    @PostConstruct
    private void init() {
        provider = Caching.getCachingProvider();
        final URI uri = getJCacheUri();
        cacheManager = provider.getCacheManager(uri, provider.getDefaultClassLoader());
    }

    // workaround until geronimo-simple-jcache 1.0.1 is out
    private URI getJCacheUri() {
        final URI uri = URI.create("geronimo:///simple-jcache.properties");
        final Field decodedPath;
        try {
            decodedPath = URI.class.getDeclaredField("decodedPath");
        } catch (final NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
        decodedPath.setAccessible(true);
        try {
            decodedPath.set(uri, "simple-jcache.properties");
        } catch (final IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        return uri;
    }

    public void invalidate(final Class<?> cacheMatcher) {
        StreamSupport.stream(cacheManager.getCacheNames().spliterator(), false)
                .filter(it -> cacheMatcher == null || it.contains(cacheMatcher.getName()))
                .forEach(it -> cacheManager.getCache(it).clear());
    }

    @Override
    public CacheResolver getCacheResolver(final CacheMethodDetails<? extends Annotation> cacheMethodDetails) {
        return findCacheResolver(cacheMethodDetails.getCacheName());
    }

    @Override
    public CacheResolver getExceptionCacheResolver(final CacheMethodDetails<CacheResult> cacheMethodDetails) {
        final String exceptionCacheName = cacheMethodDetails.getCacheAnnotation().exceptionCacheName();
        if (exceptionCacheName.isEmpty()) {
            throw new IllegalArgumentException("CacheResult.exceptionCacheName() not specified");
        }
        return findCacheResolver(exceptionCacheName);
    }

    private CacheResolver findCacheResolver(final String name) {
        Cache<?, ?> cache = cacheManager.getCache(name);
        if (cache == null) {
            cache = createCache(name);
        }
        return new CacheResolverImpl(cache);
    }

    private Cache<?, ?> createCache(final String name) {
        try {
            cacheManager.createCache(name, createConfiguration(name));
        } catch (final CacheException ce) {
            // no-op: concurrent creation
        }
        return cacheManager.getCache(name);
    }

    private Configuration<Object, Object> createConfiguration(final String name) {
        final MutableConfiguration<Object, Object> baseConfig = new MutableConfiguration<>().setStoreByValue(false);
        if (name.startsWith("com.github.rmannibucau.rblog.jaxrs.PostResource.getPages")) {
            baseConfig.setExpiryPolicyFactory(new FactoryBuilder.SingletonFactory<>(
                    TouchedExpiryPolicy.factoryOf(Duration.ONE_MINUTE)).create());
        } else { // manual eviction on write operations
            baseConfig.setExpiryPolicyFactory(new FactoryBuilder.SingletonFactory<>(EternalExpiryPolicy.factoryOf()).create());
        }
        return baseConfig;
    }

    @PreDestroy
    private void destroy() {
        cacheManager.close();
        provider.close();
    }
}
