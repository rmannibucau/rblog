package com.github.rmannibucau.rblog.jaxrs;

import com.github.rmannibucau.rblog.jaxrs.model.CategoryModel;
import com.github.rmannibucau.rblog.jpa.Category;
import com.github.rmannibucau.rblog.security.web.SecurityFilter;
import com.github.rmannibucau.rblog.test.RBlog;
import org.apache.openejb.testing.Application;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.net.ssl.HttpsURLConnection;
import javax.persistence.EntityManager;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RBlog.Runner.class)
public class CategoryResourceTest {
    @Application
    private RBlog blog;

    @Inject
    private EntityManager entityManager;

    @After
    public void clean() {
        blog.clean();
    }

    @Test
    public void getRoots() {
        create10Categories();

        final List<CategoryModel> categories = blog.target().path("category/roots").request()
            .get(new GenericType<List<CategoryModel>>() {});

        assertEquals(7, categories.size());
        IntStream.range(0, 10).filter(i -> (i % 3) != 2).forEach(i -> {
            final int catIdx = i - (i > 2 ? (i > 5 ? (i > 8 ? 3 : 2) : 1) : 0);
            assertEquals("Test Category " + i, categories.get(catIdx).getName());
            assertEquals("test-category-" + i, categories.get(catIdx).getSlug());
        });
    }

    @Test
    public void getAll() {
        create10Categories();

        final List<CategoryModel> categories = blog.target().path("category/all").request()
            .get(new GenericType<List<CategoryModel>>() {});

        assertEquals(10, categories.size());
        IntStream.range(0, 10).forEach(i -> {
            assertEquals("Test Category " + i, categories.get(i).getName());
            assertEquals("test-category-" + i, categories.get(i).getSlug());
        });
    }

    @Test
    public void get() {
        final Category category = blog.inTx(() -> {
            final Category c = new Category();
            c.setName("Test Category");
            c.setSlug("test-category");
            entityManager.persist(c);
            entityManager.flush();
            return c;
        });

        final CategoryModel model = blog.target().path("category/{id}")
            .resolveTemplate("id", category.getId()).request()
            .get(CategoryModel.class);

        assertEquals(category.getId(), model.getId());
        assertEquals(category.getName(), model.getName());
        assertEquals("test-category", model.getSlug());
        assertTrue(model.getChildren().isEmpty());
        assertNull(model.getParent());
    }

    @Test
    public void getBySlug() {
        final Category category = blog.inTx(() -> {
            final Category c = new Category();
            c.setName("Test Category");
            c.setSlug("test-category");
            entityManager.persist(c);
            entityManager.flush();
            return c;
        });

        final CategoryModel model = blog.target().path("category/slug/{slug}")
            .resolveTemplate("slug", category.getSlug()).request()
            .get(CategoryModel.class);

        assertEquals(category.getId(), model.getId());
        assertEquals(category.getName(), model.getName());
        assertEquals("test-category", model.getSlug());
        assertTrue(model.getChildren().isEmpty());
        assertNull(model.getParent());
    }

    @Test
    public void crud() {
        blog.withTempUser((user, token) -> {
            long id;
            { // create
                final CategoryModel model = blog.target().path("category").request()
                    .header(SecurityFilter.SECURITY_HEADER, token)
                    .post(Entity.entity(new CategoryModel(0, "the cat", null, "#ff0000", null, null, 0), MediaType.APPLICATION_JSON_TYPE), CategoryModel.class);

                id = model.getId();
                assertTrue(id != 0);
                assertEquals("the cat", model.getName());
                assertEquals("the-cat", model.getSlug());
                assertEquals("#ff0000", model.getColor());
                assertTrue(model.getChildren().isEmpty());
                assertNull(model.getParent());
                assertNotNull(entityManager.find(Category.class, id));
            }
            { // update
                final CategoryModel model = blog.target().path("category").request()
                    .header(SecurityFilter.SECURITY_HEADER, token)
                    .post(Entity.entity(new CategoryModel(id, "another", "test", "#ffff00", null, null, 1), MediaType.APPLICATION_JSON_TYPE), CategoryModel.class);

                assertEquals(id, model.getId());
                assertEquals("another", model.getName());
                assertEquals("test", model.getSlug());
                assertEquals("#ffff00", model.getColor());
                assertTrue(model.getChildren().isEmpty());
                assertNull(model.getParent());
                assertNotNull(entityManager.find(Category.class, id));
            }
            { // delete
                assertEquals(
                    HttpsURLConnection.HTTP_NO_CONTENT,
                    blog.target().path("category/{id}").resolveTemplate("id", id).request().header(SecurityFilter.SECURITY_HEADER, token).delete().getStatus());
                assertNull(entityManager.find(Category.class, id));
            }
        });
    }

    @Test
    public void hierarchy() {
        blog.withTempUser((user, token) -> {
            CategoryModel parent;
            CategoryModel child;

            long id;
            { // create
                parent = blog.target().path("category").request()
                    .header(SecurityFilter.SECURITY_HEADER, token)
                    .post(Entity.entity(new CategoryModel(0, "the cat", null, null, null, null, 0), MediaType.APPLICATION_JSON_TYPE), CategoryModel.class);

                id = parent.getId();
                assertTrue(id != 0);
                assertEquals("the cat", parent.getName());
                assertEquals("the-cat", parent.getSlug());
                assertTrue(parent.getChildren().isEmpty());
                assertNull(parent.getParent());
                assertNotNull(entityManager.find(Category.class, id));
            }
            { // link (parent)
                child = blog.target().path("category").request()
                    .header(SecurityFilter.SECURITY_HEADER, token)
                    .post(Entity.entity(new CategoryModel(0, "another", "test", null, parent, null, 0), MediaType.APPLICATION_JSON_TYPE), CategoryModel.class);

                assertEquals("another", child.getName());
                assertEquals("test", child.getSlug());
                assertTrue(child.getChildren().isEmpty());
                assertEquals(parent.getId(), child.getParent().getId());

                parent = blog.target().path("category/{id}").resolveTemplate("id", parent.getId()).request()
                    .header(SecurityFilter.SECURITY_HEADER, token)
                    .get(CategoryModel.class);
                assertEquals(1, parent.getChildren().size());
                assertEquals("another", parent.getChildren().iterator().next().getName());
            }
            { // unlink
                child.setParent(null);
                blog.target().path("category").request()
                    .header(SecurityFilter.SECURITY_HEADER, token)
                    .post(Entity.entity(child, MediaType.APPLICATION_JSON_TYPE), CategoryModel.class);

                assertTrue(blog.target().path("category/{id}").resolveTemplate("id", parent.getId()).request()
                    .header(SecurityFilter.SECURITY_HEADER, token)
                    .get(CategoryModel.class).getChildren().isEmpty());
            }
        });
    }

    @Test(expected = ForbiddenException.class)
    public void postNotLogged() {
        blog.target().path("category").resolveTemplate("id", 5).request()
            .post(Entity.entity(new CategoryModel(), MediaType.APPLICATION_JSON_TYPE), CategoryModel.class);
    }

    @Test(expected = ForbiddenException.class)
    public void deleteNotLogged() {
        blog.target().path("category/{id}").resolveTemplate("id", 5).request().delete(String.class);
    }

    private void create10Categories() {
        blog.inTx(() -> {
            final AtomicReference<Category> previous = new AtomicReference<>();

            IntStream.range(0, 10).forEach(i -> {
                final Category c = new Category();
                c.setName("Test Category " + i);
                c.setSlug("test-category-" + i);
                if (i % 3 == 2) {
                    c.setParent(previous.get());
                }

                entityManager.persist(c);
                previous.set(c);
            });

            entityManager.flush();
        });
    }
}
