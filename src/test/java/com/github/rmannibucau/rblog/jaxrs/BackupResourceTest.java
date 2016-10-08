package com.github.rmannibucau.rblog.jaxrs;

import com.github.rmannibucau.rblog.security.web.SecurityFilter;
import com.github.rmannibucau.rblog.test.RBlog;
import org.apache.openejb.testing.Application;
import org.apache.ziplock.IO;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(RBlog.Runner.class)
public class BackupResourceTest {
    @Application
    private RBlog blog;

    @Test
    public void download() {
        blog.withTempUser((id, token) -> {
            final File file = new File("target/BackupResourceTest" + System.nanoTime() + ".zip");
            try (final InputStream in = blog.target().path("backup")
                    .request(APPLICATION_OCTET_STREAM)
                    .header(SecurityFilter.SECURITY_HEADER, token)
                    .get(InputStream.class);
                 final OutputStream out = new FileOutputStream(file)) {
                IO.copy(in, out);
            } catch (final IOException e) {
                fail(e.getMessage());
            }
            assertTrue(file.isFile());
            try (final ZipFile zip = new ZipFile(file)) {
                final List<? extends ZipEntry> files = Collections.list(zip.entries());
                assertEquals(1, files.size());
                final ZipEntry entry = files.iterator().next();
                assertTrue(entry.getName().startsWith("backup.json"));
            } catch (final IOException e) {
                fail(e.getMessage());
            }
        });
    }
}
