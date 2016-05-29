package com.github.rmannibucau.rblog.security.service;

import javax.enterprise.context.ApplicationScoped;
import java.util.UUID;

// keep it simple for now
@ApplicationScoped
public class TokenGenerator {
    public String next() {
        return UUID.randomUUID().toString();
    }
}
