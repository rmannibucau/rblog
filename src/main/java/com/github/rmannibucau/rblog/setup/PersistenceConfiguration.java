package com.github.rmannibucau.rblog.setup;

import lombok.Getter;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@ApplicationScoped
public class PersistenceConfiguration {
    @Getter(onMethod = @_(@Produces))
    @PersistenceContext(unitName = "rblog")
    private EntityManager entityManager;
}
