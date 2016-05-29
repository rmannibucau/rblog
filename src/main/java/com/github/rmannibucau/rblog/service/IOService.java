package com.github.rmannibucau.rblog.service;

import javax.enterprise.context.ApplicationScoped;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@ApplicationScoped
public class IOService {
    public byte[] read(final InputStream stream) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] buffer = new byte[8 * 1024];
        int read;

        while ((read = stream.read(buffer)) >= 0) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }
}
