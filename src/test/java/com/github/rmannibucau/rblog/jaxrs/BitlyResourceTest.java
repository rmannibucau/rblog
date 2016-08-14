package com.github.rmannibucau.rblog.jaxrs;

import com.github.rmannibucau.rblog.jaxrs.BitlyResource;
import com.github.rmannibucau.rblog.security.web.SecurityFilter;
import com.github.rmannibucau.rblog.test.RBlog;
import org.apache.openejb.testing.Application;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.client.Entity;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RBlog.Runner.class)
public class BitlyResourceTest {
    @Application
    private RBlog blog;

    @Test
    public void isActive() {
        blog.withTempUser((id, token) -> assertTrue(blog.target().path("bitly/state")
                .request(APPLICATION_JSON_TYPE)
                .header(SecurityFilter.SECURITY_HEADER, token)
                .get(BitlyResource.State.class).isActive()));
    }

    @Test
    public void shorten() {
        blog.withTempUser((id, token) -> assertEquals("http://short/bitly", blog.target().path("bitly/shorten")
                .request(APPLICATION_JSON_TYPE)
                .header(SecurityFilter.SECURITY_HEADER, token)
                .post(Entity.entity(new BitlyResource.Url("http://test#/foo"), APPLICATION_JSON_TYPE), BitlyResource.Url.class).getValue()));
    }
}
