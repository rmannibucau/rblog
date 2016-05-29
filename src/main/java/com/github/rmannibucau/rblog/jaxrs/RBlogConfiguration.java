package com.github.rmannibucau.rblog.jaxrs;

import com.github.rmannibucau.rblog.setup.AnalyticsConfiguration;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("configuration")
@ApplicationScoped
public class RBlogConfiguration {
    @Inject
    private AnalyticsConfiguration analyticsConfiguration;

    private Configuration configuration;

    @PostConstruct
    private void init() {
        this.configuration = new Configuration(analyticsConfiguration.getCode());
    }

    @GET
    public Configuration get() {
        return this.configuration;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Configuration {
        private String analytics;
    }
}
