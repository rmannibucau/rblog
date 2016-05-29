package com.github.rmannibucau.rblog.service;

import com.github.rmannibucau.rblog.jpa.Token;
import com.github.rmannibucau.rblog.test.RBlog;
import org.apache.openejb.testing.Application;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNull;

@RunWith(RBlog.Runner.class)
public class TokenEvictionTest {
    @Application
    private RBlog blog;

    @Inject
    private TokenEviction evictor;

    @PersistenceContext
    private EntityManager em;

    @Test
    public void evict() {
        final String id = blog.inTx(() -> {
            final Token token = new Token();
            token.setValue(UUID.randomUUID().toString());
            em.persist(token);
            // hack last usage to ensure we find this token
            token.setLastUsage(new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(32)));
            return token.getValue();
        });
        evictor.evict(null);
        blog.inTx(() -> assertNull(em.find(Token.class, id))); // another tx for isolation whatever happent before
    }
}
