package com.github.rmannibucau.rblog.service;

import javax.enterprise.context.ApplicationScoped;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@ApplicationScoped
public class URLService {
    public String percentEncode(final String s) {
        try {
            return URLEncoder.encode(s, "UTF-8").replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
        } catch (final UnsupportedEncodingException uee) {
            throw new IllegalStateException(uee.getMessage(), uee);
        }
    }
}
