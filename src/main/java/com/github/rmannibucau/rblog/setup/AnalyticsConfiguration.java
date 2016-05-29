package com.github.rmannibucau.rblog.setup;

import com.github.rmannibucau.rblog.configuration.Configuration;
import lombok.Getter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class AnalyticsConfiguration {
    @Inject
    @Getter
    @Configuration("rblog.analytics.code")
    private String code;
}
