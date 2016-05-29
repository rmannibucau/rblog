package com.github.rmannibucau.rblog.service;

import com.github.rmannibucau.rblog.configuration.Configuration;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@ApplicationScoped
public class PasswordService {
    @Inject
    @Configuration("${rblog.security.password.hash_algorithm:SHA-256}")
    private String hash;

    @Inject
    @Configuration("${rblog.security.password.round_trip:3}")
    private Integer roundTrip;

    @PostConstruct
    private void valid() {
        try {
            MessageDigest.getInstance(hash);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public String toDatabaseFormat(final String from) {
        final MessageDigest instance = getMessageDigest();

        byte[] result = from.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < roundTrip; i++) {
            instance.reset();
            instance.update(result);
            result = instance.digest();
        }

        return DatatypeConverter.printHexBinary(result);
    }

    private MessageDigest getMessageDigest() {
        final MessageDigest instance;
        try {
            instance = MessageDigest.getInstance(hash);
        } catch (NoSuchAlgorithmException e) { // post construct ensures we don't go there
            throw new IllegalStateException("Can't get message digest", e);
        }
        return instance;
    }
}
