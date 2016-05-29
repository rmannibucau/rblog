package com.github.rmannibucau.rblog.security;

import com.github.rmannibucau.rblog.jpa.Token;
import com.github.rmannibucau.rblog.security.web.SecurityFilter;
import com.github.rmannibucau.rblog.security.web.model.Credentials;
import com.github.rmannibucau.rblog.security.web.model.TokenValue;
import com.github.rmannibucau.rblog.test.RBlog;
import org.apache.openejb.testing.Application;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.net.ssl.HttpsURLConnection;
import javax.persistence.EntityManager;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(RBlog.Runner.class)
public class TokenValueResourceTest {
    @Application
    private RBlog blog;

    @Inject
    private EntityManager entityManager;

    private long userId;

    @Before
    public void addTempUser() {
        userId = blog.createUser("usr", "pwd").getId();
    }

    @After
    public void removeTempUser() {
        blog.deleteUser(userId);
    }

    @Test
    public void loginOk() {
        final Response response = blog.target()
            .path("security/login").request()
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.entity(new Credentials() {
                {
                    setUsername("usr");
                    setPassword("pwd");
                }
            }, MediaType.APPLICATION_JSON_TYPE));
        assertEquals(HttpsURLConnection.HTTP_OK, response.getStatus());

        final String token = response.readEntity(TokenValue.class).getToken();
        assertNotNull(token);
        assertNotNull(entityManager.find(Token.class, token));

        blog.inTx(() -> { // cleanup
            entityManager.remove(entityManager.find(Token.class, token));
        });
    }

    @Test
    public void loginKo() {
        final Response response = blog.target()
            .path("security/login").request()
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.entity(new Credentials() {
                {
                    setUsername("bad");
                    setPassword("pwd");
                }
            }, MediaType.APPLICATION_JSON_TYPE));
        assertEquals(HttpsURLConnection.HTTP_BAD_REQUEST, response.getStatus());
    }

    @Test
    public void logout() {
        final TokenValue tokenValue = blog.target()
            .path("security/login").request()
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.entity(new Credentials() {
                {
                    setUsername("usr");
                    setPassword("pwd");
                }
            }, MediaType.APPLICATION_JSON_TYPE), TokenValue.class);
        assertNotNull(entityManager.find(Token.class, tokenValue.getToken()));

        assertEquals(
            HttpsURLConnection.HTTP_NO_CONTENT,
            blog.target()
                .path("security/logout").request()
                .header(SecurityFilter.SECURITY_HEADER, tokenValue.getToken())
                .head().getStatus());
        assertNull(entityManager.find(Token.class, tokenValue.getToken()));
    }

    @Test
    public void empty() {
        final TokenValue[] values = new TokenValue[5];
        IntStream.range(0, 5).forEach(i -> values[i] = blog.target()
            .path("security/login").request()
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.entity(new Credentials() {
                {
                    setUsername("usr");
                    setPassword("pwd");
                }
            }, MediaType.APPLICATION_JSON_TYPE), TokenValue.class));


        assertEquals(
            HttpsURLConnection.HTTP_NO_CONTENT,
            blog.target()
                .path("security/empty").request()
                .header(SecurityFilter.SECURITY_HEADER, values[3].getToken())
                .head().getStatus());
        IntStream.range(0, 5).filter(i -> i != 3).forEach(i -> assertNull(entityManager.find(Token.class, values[i].getToken())));
        assertNotNull(entityManager.find(Token.class, values[3].getToken()));
    }
}
