package com.github.rmannibucau.rblog.setup;

import com.github.rmannibucau.rblog.configuration.Configuration;
import com.github.rmannibucau.rblog.jpa.User;
import com.github.rmannibucau.rblog.service.PasswordService;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.transaction.Transactional;

import static com.github.rmannibucau.rblog.lang.Exceptions.safe;

public class DefaultUser {
    @Inject
    @Configuration("${rblog.provisioning.defaultUser.active:true}")
    private Boolean defaultUser;

    @Inject
    @Configuration("${rblog.provisioning.defaultUser.name:admin}")
    private String name;

    @Inject
    private EntityManager entityManager;

    @Inject
    private PasswordService passwordService;

    @Transactional
    void insertAdmin(@Observes @Initialized(ApplicationScoped.class) final Object init) {
        if (!defaultUser) {
            return;
        }

        if (safe(
            () -> entityManager.createQuery("select count(u) from User u", Number.class).getSingleResult(),
            () -> 0,
            NoResultException.class).longValue() != 0) {
            return;
        }

        final User u = new User();
        u.setUsername(name);
        u.setPassword(passwordService.toDatabaseFormat(name));
        entityManager.persist(u);
    }
}
