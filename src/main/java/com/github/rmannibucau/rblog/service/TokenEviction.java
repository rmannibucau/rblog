package com.github.rmannibucau.rblog.service;

import com.github.rmannibucau.rblog.configuration.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static java.util.Optional.ofNullable;

@Startup
@Singleton
public class TokenEviction {
    @Resource
    private TimerService timerService;

    @Inject
    @Configuration("${rblog.token.eviction:P30D}") // java.time.Duration syntax, 30 days = PT720H
    private String validDuration;

    @Inject
    @Configuration("${rblog.token.eviction:P1D}")
    private String evictionTimeout;

    @PersistenceContext
    private EntityManager entityManager;

    private Timer timer;
    private long durationMs;

    @PostConstruct
    private void startEviction() {
        durationMs = ofNullable(validDuration)
                .map(d -> Duration.parse(d).toMillis())
                .orElse(TimeUnit.DAYS.toMillis(30));
        final long timeout = ofNullable(evictionTimeout)
                .map(d -> Duration.parse(d).toMillis())
                .orElse(TimeUnit.DAYS.toMillis(1));
        if (timeout > 0 && durationMs > 0) {
            timer = timerService.createIntervalTimer(timeout, timeout, new TimerConfig("token-eviction", false));
        }
    }

    @PreDestroy
    private void shutdown() {
        ofNullable(timer).ifPresent(Timer::cancel);
    }

    @Timeout
    public void evict(final Timer timer) {
        entityManager.createNamedQuery("Token.deleteExpiredTokens")
                .setParameter("date", new Date(System.currentTimeMillis() - durationMs))
                .executeUpdate();
    }
}
