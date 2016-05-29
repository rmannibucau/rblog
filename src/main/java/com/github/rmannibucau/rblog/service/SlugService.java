package com.github.rmannibucau.rblog.service;

import javax.enterprise.context.ApplicationScoped;
import java.text.Normalizer;
import java.util.Locale;

@ApplicationScoped
public class SlugService {
    public String slugFrom(final String title) {
        return Normalizer.normalize(title.trim(), Normalizer.Form.NFD)
            .replaceAll("[^\\p{ASCII}]", "")
            .replaceAll("[^\\w+]", "-")
            .replaceAll("\\s+", "-")
            .replaceAll("[-]+", "-")
            .replaceAll("^-", "")
            .replaceAll("-$", "")
            .toLowerCase(Locale.ENGLISH);
    }
}
