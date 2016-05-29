package com.github.rmannibucau.rblog.jaxrs.provider;

import org.apache.johnzon.jaxrs.JohnzonProvider;
import org.apache.johnzon.mapper.MapperBuilder;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.ext.Provider;

@Provider
@Produces({"application/json", "application/*+json"})
@Consumes({"application/json", "application/*+json"})
public class JSonProvider<T> extends JohnzonProvider<T> {
    public JSonProvider() { // we use lombok and decorate fields so ensure we don't use method reader
        super(new MapperBuilder().setAccessModeName("field").build(), null);
    }
}
