package com.github.rmannibucau.rblog.service;

import com.github.rmannibucau.rblog.configuration.Configuration;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.io.Closeable;
import java.io.IOException;

import static java.util.Optional.ofNullable;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

@ApplicationScoped
public class BitlyService {
    @Inject
    @Configuration("rblog.bitly.group")
    private String group;

    @Inject
    @Configuration("rblog.bitly.token")
    private String token;

    @Inject
    @Configuration("${rblog.bitly.domain:bit.ly}")
    private String domain;

    @Inject
    @Configuration("${rblog.bitly.url:https://api-ssl.bitly.com/v4/shorten}")
    private String url;

    private WebTarget target;
    private Client client;

    @PostConstruct
    private void init() {
        client = ClientBuilder.newBuilder().build();
        target = client.target(url);
    }

    @PreDestroy
    private void destroy() {
        ofNullable(client).filter(Closeable.class::isInstance).ifPresent(c -> {
            try {
                Closeable.class.cast(c).close();
            } catch (final IOException e) {
                // no-op
            }
        });
    }

    public boolean isActive() {
        return token != null;
    }

    public String bitlyize(final String toShorten) {
        return target
                .request(APPLICATION_JSON_TYPE)
                .header("Authorization", "Bearer " + token)
                .post(entity(new BitlyData(group, domain, toShorten), APPLICATION_JSON_TYPE), BitlyResponse.class)
                .getLink();
    }

    @Data
    public static class BitlyResponse {
        private String link;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BitlyData {
        private String group_guid;
        private String domain;
        private String long_url;
    }
}
