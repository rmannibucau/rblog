package com.github.rmannibucau.rblog.jaxrs;

import com.github.rmannibucau.rblog.jaxrs.model.UserModel;
import com.github.rmannibucau.rblog.jpa.User;
import com.github.rmannibucau.rblog.security.web.SecurityFilter;
import com.github.rmannibucau.rblog.service.Backup;
import com.github.rmannibucau.rblog.test.RBlog;
import org.apache.openejb.testing.Application;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.net.ssl.HttpsURLConnection;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(RBlog.Runner.class)
public class UserResourceTest {
    @Application
    private RBlog blog;

    @Inject
    private Backup.Internal backup;

    @Before
    public void noMail() {
        backup.setDynamicActive(false);
        blog.clean();
    }

    @After
    public void clean() {
        blog.clean();
        backup.setDynamicActive(true);
    }

    @Test
    public void getAll() {
        IntStream.range(0, 5).forEach(i -> blog.createUser("test_" + i, "test"));
        blog.withTempUser((userId, token) -> {
            final List<UserModel> users = blog.target().path("user").request()
                .header(SecurityFilter.SECURITY_HEADER, token)
                .get(new GenericType<List<UserModel>>() {});

            assertEquals(6, users.size());
            assertEquals("test", users.get(0).getUsername()); // temp user
            IntStream.range(0, 5).forEach(i -> {
                final UserModel user = users.get(1 + i);
                assertEquals(user.getUsername(), "test_" + i);
                assertEquals(user.getDisplayName(), "test_" + i);
            });
        });
    }

    @Test
    public void get() {
        final User user = blog.createUser("test", "test");
        blog.withToken("test", "test", token -> {
            final UserModel model = blog.target().path("user/{id}")
                .resolveTemplate("id", user.getId()).request()
                .header(SecurityFilter.SECURITY_HEADER, token)
                .get(UserModel.class);

            assertEquals(user.getId(), model.getId());
            assertEquals(user.getUsername(), model.getUsername());
            assertEquals(user.getDisplayName(), model.getDisplayName());
            assertEquals(user.getMail(), model.getMail());
            assertNull(model.getPassword()); // not read, just used for updates
        });
    }

    @Test
    public void crud() {
        blog.withTempUser((user, token) -> {
            long id;
            { // create
                final UserModel model = blog.target().path("user").request()
                    .header(SecurityFilter.SECURITY_HEADER, token)
                    .post(Entity.entity(new UserModel(0, "first", "First", "first@second.third", "secret", 0), MediaType.APPLICATION_JSON_TYPE), UserModel.class);

                id = model.getId();
                assertTrue(id != 0);
                assertEquals("first", model.getUsername());
                assertEquals("First", model.getDisplayName());
                assertEquals("first@second.third", model.getMail());
                assertNull(model.getPassword());
            }
            { // check we can get a token
                blog.withToken("first", "secret", Assert::assertNotNull);
            }
            { // update
                final UserModel model = blog.target().path("user").request()
                    .header(SecurityFilter.SECURITY_HEADER, token)
                    .post(Entity.entity(new UserModel(id, "second", "Second", "second@third.fourth", "stillsecret", 1), MediaType.APPLICATION_JSON_TYPE), UserModel.class);

                id = model.getId();
                assertTrue(id != 0);
                assertEquals("second", model.getUsername());
                assertEquals("Second", model.getDisplayName());
                assertEquals("second@third.fourth", model.getMail());
                assertNull(model.getPassword());
            }
            { // check we can get a token only with the new password
                try {
                    blog.withToken("first", "secret", Assert::assertNull);
                    fail();
                } catch (final BadRequestException bre) {
                    // ok
                }
                blog.withToken("second", "stillsecret", Assert::assertNotNull);
            }
            { // delete
                assertEquals(
                    HttpsURLConnection.HTTP_NO_CONTENT,
                    blog.target().path("user/{id}").resolveTemplate("id", id).request().header(SecurityFilter.SECURITY_HEADER, token).delete().getStatus());
            }
        });
    }

    @Test
    public void getNotLogged() {
        assertEquals(HttpsURLConnection.HTTP_FORBIDDEN, blog.target().path("user/{id}").resolveTemplate("id", 5).request().get().getStatus());
    }
}
