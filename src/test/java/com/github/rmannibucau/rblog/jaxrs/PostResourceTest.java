package com.github.rmannibucau.rblog.jaxrs;

import com.github.rmannibucau.rblog.jaxrs.model.CategoryModel;
import com.github.rmannibucau.rblog.jaxrs.model.NotificationModel;
import com.github.rmannibucau.rblog.jaxrs.model.PostModel;
import com.github.rmannibucau.rblog.jaxrs.model.PostPage;
import com.github.rmannibucau.rblog.jaxrs.model.TopPosts;
import com.github.rmannibucau.rblog.jaxrs.model.UserModel;
import com.github.rmannibucau.rblog.jpa.Attachment;
import com.github.rmannibucau.rblog.jpa.Category;
import com.github.rmannibucau.rblog.jpa.Notification;
import com.github.rmannibucau.rblog.jpa.Post;
import com.github.rmannibucau.rblog.jpa.PostType;
import com.github.rmannibucau.rblog.jpa.User;
import com.github.rmannibucau.rblog.security.web.SecurityFilter;
import com.github.rmannibucau.rblog.service.Backup;
import com.github.rmannibucau.rblog.service.PasswordService;
import com.github.rmannibucau.rblog.test.RBlog;
import org.apache.openejb.testing.Application;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;
import javax.net.ssl.HttpsURLConnection;
import javax.persistence.EntityManager;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.lang.Thread.sleep;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(RBlog.Runner.class)
public class PostResourceTest {
    @Application
    private RBlog blog;

    @Inject
    private PasswordService passwordService;

    @Inject
    private EntityManager entityManager;

    @Inject
    private Backup backup;

    private User[] users;
    private Post[] posts;
    private Category[] categories;
    private long startTime;

    @Before
    @After
    public void resetDB() {
        blog.clean();
    }

    private Post createPostWithPublishedDate(final long date) {
        return blog.inTx(() -> {
            final User u = new User();
            u.setUsername("author");
            u.setPassword(passwordService.toDatabaseFormat("author"));
            entityManager.persist(u);

            final Post post = new Post();
            post.setAuthor(u);
            post.setSlug("new-post");
            post.setTitle("New post");
            post.setContent("some content");
            post.setPublishDate(new Date(date));
            entityManager.persist(post);

            return post;
        });
    }

    public void createMultiplePosts() {
        blog.inTx(() -> {
            users = new User[2];
            IntStream.range(0, users.length)
                .forEach(i -> {
                    final User user = new User();
                    user.setDisplayName("Foo Bar #" + i);
                    user.setMail("foo" + i + "@bar.com");
                    user.setPassword(passwordService.toDatabaseFormat("secret" + i));
                    user.setUsername("foo" + i);
                    entityManager.persist(user);
                    users[i] = user;
                });

            categories = new Category[2];
            IntStream.range(0, categories.length)
                .forEach(i -> {
                    final Category category = new Category();
                    category.setName("Category " + i);
                    category.setSlug("category-" + i);
                    entityManager.persist(category);
                    categories[i] = category;
                });

            startTime = System.currentTimeMillis();
            posts = new Post[10];
            IntStream.range(0, posts.length)
                .forEach(i -> {
                    final Post post = new Post();
                    post.setContent("bla bla #" + i);
                    post.setPublishDate(new Date(startTime + (i % 3 == 0 ? -1 : 1) * TimeUnit.MINUTES.toMillis(5)));
                    post.setTitle("bla");
                    post.setSlug("bla-" + i);
                    post.setType(i % 6 != 0 ? PostType.POST : PostType.PAGE);
                    post.setAuthor(users[i % users.length]);
                    entityManager.persist(post);
                    posts[i] = post;

                    if (i % 4 == 0) {
                        final Category category = categories[(i / 4) % categories.length];

                        post.setCategories(new HashSet<>());
                        post.getCategories().add(category);

                        category.setPosts(ofNullable(category.getPosts()).orElse(new HashSet<>()));
                        category.getPosts().add(post);
                    }
                });
        });
    }

    @Test
    public void getTop() {
        createMultiplePosts();
        final TopPosts posts = blog.target()
            .path("post/top")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .get(TopPosts.class);
        assertEquals(3, posts.getLasts().size());
        posts.getLasts().stream().forEach(p -> assertNull(p.getContent())); // not loaded for this endpoint
        assertEquals(2, posts.getByCategories().size());

        final TopPosts.CategoryData category0 = posts.getByCategories().get("Category 0");
        assertEquals("category-0", category0.getSlug());
        assertNull(category0.getColor());
        assertEquals(1, category0.getPosts().size());
        assertTrue(posts.getByCategories().get("Category 1").getPosts().isEmpty()); // all are not published
    }

    @Test
    public void getAllLogged() {
        createMultiplePosts();
        blog.withTempUser((u, token) -> {
            final PostPage posts = blog.target()
                .path("post/select")
                .queryParam("type", "all")
                .queryParam("orderBy", "none")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header(SecurityFilter.SECURITY_HEADER, token)
                .get(PostPage.class);
            assertEquals(10, posts.getTotal());
            assertEquals(
                posts.getRows().stream().map(PostModel::getId).collect(toSet()),
                Stream.of(this.posts).map(Post::getId).collect(toSet()));
        });
    }

    @Test
    public void getAll() {
        createMultiplePosts();
        final Date now = new Date();
        final PostPage posts = blog.target()
            .path("post/select")
            .queryParam("type", "all")
            .queryParam("orderBy", "none")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .get(PostPage.class);
        assertEquals(4, posts.getTotal());
        IntStream.range(0, 4).forEach(i -> assertTrue(posts.getRows().get(i).getPublished().before(now)));
    }

    @Test
    public void search() {
        createMultiplePosts();
        blog.withTempUser((u, token) -> { // all have bla in the title
            final PostPage posts = blog.target()
                .path("post/select")
                .queryParam("type", "all")
                .queryParam("orderBy", "title")
                .queryParam("search", "bla")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header(SecurityFilter.SECURITY_HEADER, token)
                .get(PostPage.class);
            assertEquals(10, posts.getTotal());
            assertEquals(
                posts.getRows().stream().map(PostModel::getId).collect(toSet()),
                Stream.of(this.posts).map(Post::getId).collect(toSet()));
        });
        blog.withTempUser((u, token) -> { // all have # in the content
            final PostPage posts = blog.target()
                .path("post/select")
                .queryParam("type", "all")
                .queryParam("orderBy", "title")
                .queryParam("search", "#")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header(SecurityFilter.SECURITY_HEADER, token)
                .get(PostPage.class);
            assertEquals(10, posts.getTotal());
            assertEquals(
                posts.getRows().stream().map(PostModel::getId).collect(toSet()),
                Stream.of(this.posts).map(Post::getId).collect(toSet()));
        });
        blog.withTempUser((u, token) -> { // only one has #5
            final PostPage posts = blog.target()
                .path("post/select")
                .queryParam("type", "all")
                .queryParam("orderBy", "title")
                .queryParam("search", "#5")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header(SecurityFilter.SECURITY_HEADER, token)
                .get(PostPage.class);
            assertEquals(1, posts.getTotal());
            assertEquals("bla bla #5", posts.getRows().iterator().next().getContent());
        });
        blog.withTempUser((u, token) -> { // by category slug
            final PostPage posts = blog.target()
                .path("post/select")
                .queryParam("categorySlug", categories[1].getSlug())
                .queryParam("orderBy", "title")
                .queryParam("type", "all")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header(SecurityFilter.SECURITY_HEADER, token)
                .get(PostPage.class);
            assertEquals(1, posts.getTotal());
            assertEquals(1, posts.getRows().size());
            assertEquals("bla bla #4", posts.getRows().iterator().next().getContent());

            // just to check we support more than one entry
            assertEquals(2, blog.target()
                .path("post/select")
                .queryParam("categorySlug", categories[0].getSlug())
                .queryParam("orderBy", "title")
                .queryParam("type", "all")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header(SecurityFilter.SECURITY_HEADER, token)
                .get(PostPage.class).getTotal());
        });
        blog.withTempUser((u, token) -> { // by category slug
            final PostPage posts = blog.target()
                .path("post/select")
                .queryParam("categoryId", categories[1].getId())
                .queryParam("orderBy", "title")
                .queryParam("type", "all")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header(SecurityFilter.SECURITY_HEADER, token)
                .get(PostPage.class);
            assertEquals(1, posts.getTotal());
            assertEquals("bla bla #4", posts.getRows().iterator().next().getContent());

            // just to check we support more than one entry
            final PostPage postPage = blog.target()
                .path("post/select")
                .queryParam("categoryId", categories[0].getId())
                .queryParam("orderBy", "title")
                .queryParam("type", "all")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header(SecurityFilter.SECURITY_HEADER, token)
                .get(PostPage.class);
            // check both request count and *
            assertEquals(2, postPage.getTotal());
            assertEquals(2, postPage.getRows().size());
        });
    }

    @Test
    public void getAllPagination() {
        createMultiplePosts();
        final int pageSize = 5;
        final WebTarget target = blog.target()
            .path("post/select")
            .queryParam("type", "all")
            .queryParam("orderBy", "slug")
            .queryParam("order", "asc")
            .queryParam("number", pageSize);
        blog.withTempUser((u, token) -> {
            IntStream.range(0, 2).forEach(pageIdx -> {
                final int offset = pageSize * pageIdx;
                final PostPage page = target
                    .queryParam("offset", offset)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .header(SecurityFilter.SECURITY_HEADER, token)
                    .get(PostPage.class);
                assertEquals(10, page.getTotal());

                final Iterator<PostModel> posts = page.getRows().iterator();
                IntStream.range(offset, offset + pageSize).forEach(i -> {
                    assertTrue(posts.hasNext());

                    final PostModel post = posts.next();
                    assertPost(this.posts[i], post);
                });
                assertFalse(posts.hasNext());
            });
        });
    }

    @Test
    public void getAllOrderBy() {
        createMultiplePosts();
        blog.withTempUser((u, token) -> {
            {
                final PostPage posts = blog.target()
                    .path("post/select")
                    .queryParam("type", "all")
                    .queryParam("orderBy", "type") // desc by default
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .header(SecurityFilter.SECURITY_HEADER, token)
                    .get(PostPage.class);
                assertEquals(10, posts.getTotal());
                IntStream.range(0, 8).forEach(i -> assertEquals(PostType.POST.name(), posts.getRows().get(i).getType()));
                IntStream.range(8, 10).forEach(i -> assertEquals(PostType.PAGE.name(), posts.getRows().get(i).getType()));
            }
            {
                final PostPage posts = blog.target()
                    .path("post/select")
                    .queryParam("type", "all")
                    .queryParam("orderBy", "type") // desc by default
                    .queryParam("order", "asc")
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .header(SecurityFilter.SECURITY_HEADER, token)
                    .get(PostPage.class);
                assertEquals(10, posts.getTotal());
                IntStream.range(0, 2).forEach(i -> assertEquals(PostType.PAGE.name(), posts.getRows().get(i).getType()));
                IntStream.range(2, 10).forEach(i -> assertEquals(PostType.POST.name(), posts.getRows().get(i).getType()));
            }
        });
    }

    @Test
    public void getAllByType() {
        createMultiplePosts();
        final PostPage posts = blog.target()
            .path("post/select")
            .queryParam("type", "page")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .get(PostPage.class);
        assertEquals(2, posts.getTotal());
        IntStream.range(0, 2).forEach(i -> assertEquals(PostType.PAGE.name(), posts.getRows().get(i).getType()));
    }

    @Test
    public void getAllByStatus() {
        createMultiplePosts();
        final Date now = new Date();
        final PostPage posts = blog.target()
            .path("post/select")
            .queryParam("status", "unpublished")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .get(PostPage.class);
        assertEquals(6, posts.getTotal());
        IntStream.range(0, 6).forEach(i -> assertTrue(posts.getRows().get(i).getPublished().after(now)));
    }

    @Test
    public void getAllByAfter() {
        createMultiplePosts();
        final long timePoint = startTime + TimeUnit.SECONDS.toMillis(2 * 3);
        blog.withTempUser((u, token) -> {
            final PostPage posts = blog.target()
                .path("post/select")
                .queryParam("after", new Date(timePoint).toInstant().toString())
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header(SecurityFilter.SECURITY_HEADER, token)
                .get(PostPage.class);
            assertEquals(6, posts.getTotal());
            IntStream.range(0, 6)
                .forEach(i -> assertTrue(TimeUnit.MILLISECONDS.toSeconds(posts.getRows().get(i).getPublished().getTime()) >= TimeUnit.MILLISECONDS.toSeconds(timePoint)));
        });
    }

    @Test
    public void getAllByBefore() {
        createMultiplePosts();
        final String timePoint = Instant.ofEpochMilli(startTime).toString();
        final PostPage posts = blog.target()
            .path("post/select")
            .queryParam("before", timePoint)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .get(PostPage.class);
        assertEquals(2, posts.getTotal());
        IntStream.range(0, 2)
            .forEach(i -> {
                assertEquals(PostType.POST.name(), posts.getRows().get(i).getType());
                assertTrue(TimeUnit.MILLISECONDS.toSeconds(posts.getRows().get(i).getPublished().getTime()) <= startTime);
            });
    }

    @Test
    public void findBySlug() {
        assertPost(
            createPostWithPublishedDate(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(1)),
            blog.target().path("post/slug/{slug}")
                .resolveTemplate("slug", "new-post").request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get(PostModel.class));
    }

    @Test(expected = ForbiddenException.class)
    public void adminFindByIdNeedsUser() {
        blog.target().path("post/admin/{id}")
            .resolveTemplate("id", 5).request()
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .get(PostModel.class);
    }

    @Test(expected = ForbiddenException.class)
    public void adminPost() {
        blog.target().path("post/admin").request()
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.entity(new PostModel(), MediaType.APPLICATION_JSON_TYPE), PostModel.class);
    }

    @Test(expected = ForbiddenException.class)
    public void adminDelete() {
        blog.target().path("post/admin/{id}").resolveTemplate("id", 18).request().delete(String.class);
    }

    @Test
    public void crud() {
        blog.withTempUser((user, token) -> {
            final Date published = new Date();

            backup.reset();
            int mailNumber = blog.getMail().getReceivedMessages().length;

            PostModel model;
            { // create
                model = blog.target().path("post/admin").request()
                    .header(SecurityFilter.SECURITY_HEADER, token)
                    .post(Entity.entity(new PostModel(
                        0, "post", null, "Title", "Summ", "Content", null, null, published,
                        new UserModel(user, null, null, null, null, 0), 0, null, null, null), MediaType.APPLICATION_JSON_TYPE), PostModel.class);

                assertNotNull(entityManager.find(Post.class, model.getId()));

                assertTrue(model.getId() > 0);
                assertEquals("POST", model.getType());
                assertEquals("Title", model.getTitle());
                assertEquals("Summ", model.getSummary());
                assertEquals("Content", model.getContent());
                assertNotNull(model.getCreated());
                assertNotNull(model.getUpdated());
                assertEquals(TimeUnit.MILLISECONDS.toMinutes(published.getTime()), TimeUnit.MILLISECONDS.toMinutes(model.getPublished().getTime()), 2);
                assertEquals("title", model.getSlug());

                // ensure backup mail was sent
                boolean mailOk = false;
                for (int i = 1; i < 10; i++) { // depending tests we can have had other mails so try to find the one we want
                    mailNumber++;
                    assertTrue(blog.getMail().waitForIncomingEmail(TimeUnit.SECONDS.toMillis(45), mailNumber));
                    final MimeMessage mails = blog.getMail().getReceivedMessages()[mailNumber - 1];
                    assertNotNull(mails);
                    try {
                        final Multipart multipart = Multipart.class.cast(mails.getContent());
                        assertEquals(1, multipart.getCount());
                        final BodyPart part = multipart.getBodyPart(0);
                        assertTrue(part.getFileName().startsWith("rblog_"));
                        assertTrue(part.getFileName().endsWith(".zip"));
                        try (final ZipInputStream zip = new ZipInputStream(part.getInputStream())) {
                            final ZipEntry nextEntry = zip.getNextEntry();
                            assertNotNull(nextEntry);
                            assertEquals("backup.json", nextEntry.getName());

                            final JsonObject content = Json.createReader(zip).readObject();
                            final String debug = content.toString();
                            Stream.of("date", "categories", "posts", "users").forEach(k -> assertTrue(debug, content.containsKey(k)));
                            Stream.of("posts", "users").forEach(k -> assertEquals(debug, 1, content.getJsonArray(k).size()));

                            final JsonObject u = content.getJsonArray("users").getJsonObject(0);
                            Stream.of("username", "displayName").forEach(k -> assertEquals(debug, "test", u.getString(k)));

                            final JsonObject p = content.getJsonArray("posts").getJsonObject(0);
                            assertEquals("Title", p.getString("title"));
                            assertEquals("Content", p.getString("content"));
                            assertEquals("Summ", p.getString("summary"));
                            assertEquals("title", p.getString("slug"));
                            assertEquals("test", p.getString("author"));
                        }
                        mailOk = true;
                        break;
                    } catch (final MessagingException | IOException e) {
                        fail(e.getMessage());
                    } catch (final AssertionError ae) {
                        System.out.println("Error, retrying: " + ae.getMessage());
                    }
                }
                assertTrue("mail for post creation was not found", mailOk);
            }
            { // update
                model.setContent("Content 2");
                model.setType("page");
                model.setTitle("Title 2");
                model.setSummary("Summ 2");
                model.setPublished(new Date(published.getTime() + TimeUnit.MINUTES.toMillis(10)));

                final long oldId = model.getId();
                model = blog.target().path("post/admin").request()
                    .header(SecurityFilter.SECURITY_HEADER, token)
                    .post(Entity.entity(model, MediaType.APPLICATION_JSON_TYPE), PostModel.class);

                assertEquals(oldId, model.getId());
                assertEquals("PAGE", model.getType());
                assertEquals("Title 2", model.getTitle());
                assertEquals("Summ 2", model.getSummary());
                assertEquals("Content 2", model.getContent());
                assertNotNull(model.getCreated());
                assertNotNull(model.getUpdated());
                assertEquals(published.getTime() + TimeUnit.MINUTES.toMillis(10), model.getPublished().getTime(), TimeUnit.MINUTES.toMillis(2));
                assertEquals("title", model.getSlug()); // not recomputed to have permalinks

                mailNumber++;
                assertTrue(blog.getMail().waitForIncomingEmail(TimeUnit.MINUTES.toMillis(1), mailNumber));
                final MimeMessage[] receivedMessages = blog.getMail().getReceivedMessages();
                final MimeMessage mail = receivedMessages[mailNumber - 1];
                assertNotNull(mail);
                try {
                    final Multipart multipart = Multipart.class.cast(mail.getContent());
                    assertEquals(1, multipart.getCount());
                    final BodyPart part = multipart.getBodyPart(0);
                    assertTrue(part.getFileName().startsWith("rblog_"));
                    assertTrue(part.getFileName().endsWith(".zip"));
                    try (final ZipInputStream zip = new ZipInputStream(part.getInputStream())) {
                        final ZipEntry nextEntry = zip.getNextEntry();
                        assertNotNull(nextEntry);
                        assertEquals("backup.json", nextEntry.getName());

                        final JsonObject content = Json.createReader(zip).readObject();
                        final String debug = content.toString();
                        Stream.of("date", "categories", "posts", "users").forEach(k -> assertTrue(debug, content.containsKey(k)));
                        Stream.of("posts", "users").forEach(k -> assertEquals(debug, 1, content.getJsonArray(k).size()));

                        final JsonObject u = content.getJsonArray("users").getJsonObject(0);
                        Stream.of("username", "displayName").forEach(k -> assertEquals(debug, "test", u.getString(k)));

                        final JsonObject p = content.getJsonArray("posts").getJsonObject(0);
                        assertEquals(debug, "Title 2", p.getString("title"));
                        assertEquals(debug, "Content 2", p.getString("content"));
                        assertEquals(debug, "Summ 2", p.getString("summary"));
                    }
                } catch (final MessagingException | IOException e) {
                    fail(e.getMessage());
                }
            }
            { // update permalink
                model.setSlug("changed-slug");
                model = blog.target().path("post/admin").request()
                    .header(SecurityFilter.SECURITY_HEADER, token)
                    .post(Entity.entity(model, MediaType.APPLICATION_JSON_TYPE), PostModel.class);

                assertEquals("PAGE", model.getType());
                assertEquals("Title 2", model.getTitle());
                assertEquals("Summ 2", model.getSummary());
                assertEquals("Content 2", model.getContent());
                assertNotNull(model.getCreated());
                assertNotNull(model.getUpdated());
                assertEquals(published.getTime() + TimeUnit.MINUTES.toMillis(10), model.getPublished().getTime(), TimeUnit.MINUTES.toMillis(2));
                assertEquals("changed-slug", model.getSlug());
            }
            { // add category
                model.setCategories(new ArrayList<>(singletonList(new CategoryModel(0, "New Category", null, null, null, null, 0))));
                model = blog.target().path("post/admin").request()
                    .header(SecurityFilter.SECURITY_HEADER, token)
                    .post(Entity.entity(model, MediaType.APPLICATION_JSON_TYPE), PostModel.class);

                assertEquals(1, model.getCategories().size());
                assertEquals("New Category", model.getCategories().iterator().next().getName());
            }
            { // add another category
                model.getCategories().add(new CategoryModel(0, "Another Category", null, null, null, null, 0));
                model = blog.target().path("post/admin").request()
                    .header(SecurityFilter.SECURITY_HEADER, token)
                    .post(Entity.entity(model, MediaType.APPLICATION_JSON_TYPE), PostModel.class);

                assertEquals(2, model.getCategories().size());

                final Iterator<CategoryModel> iterator = model.getCategories().iterator();
                assertEquals("Another Category", iterator.next().getName());
                assertEquals("New Category", iterator.next().getName());
            }
            { // attachments are retrieved
                assertTrue(model.getAttachments().isEmpty());

                final Attachment attachment = new Attachment();
                attachment.setContent("test".getBytes(StandardCharsets.UTF_8));
                attachment.setPost(entityManager.find(Post.class, model.getId()));
                final long id = blog.inTx(() -> {
                    entityManager.persist(attachment);
                    return attachment.getId();
                });

                model = blog.target().path("post/admin").request()
                    .header(SecurityFilter.SECURITY_HEADER, token)
                    .post(Entity.entity(model, MediaType.APPLICATION_JSON_TYPE), PostModel.class);
                assertEquals(1, model.getAttachments().size());
                assertEquals(id, model.getAttachments().iterator().next().getId());
            }
            { // remove category
                final Iterator<CategoryModel> iterator = model.getCategories().iterator();
                iterator.next();
                iterator.next();
                iterator.remove();

                model = blog.target().path("post/admin").request()
                    .header(SecurityFilter.SECURITY_HEADER, token)
                    .post(Entity.entity(model, MediaType.APPLICATION_JSON_TYPE), PostModel.class);

                assertEquals(1, model.getCategories().size());
                assertEquals("Another Category", model.getCategories().iterator().next().getName());
            }
            { // add notification
                final Date when = new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(20));
                model.setNotification(new NotificationModel("test #yeah", when));
                model = blog.target().path("post/admin").request()
                    .header(SecurityFilter.SECURITY_HEADER, token)
                    .post(Entity.entity(model, MediaType.APPLICATION_JSON_TYPE), PostModel.class);

                final Notification notification = entityManager.find(Notification.class, model.getId());
                assertNotNull(notification);
                assertEquals("test #yeah", notification.getText());
                assertEquals(when.getTime(), notification.getDate().getTime(), TimeUnit.MINUTES.toMillis(2));
                assertEquals(Notification.State.TO_PUBLISH, notification.getState());
            }
            { // delete notification
                model.setNotification(null);
                model = blog.target().path("post/admin").request()
                    .header(SecurityFilter.SECURITY_HEADER, token)
                    .post(Entity.entity(model, MediaType.APPLICATION_JSON_TYPE), PostModel.class);
                assertNull(entityManager.find(Notification.class, model.getId()));
            }
            { // delete
                assertEquals(
                    HttpsURLConnection.HTTP_NO_CONTENT,
                    blog.target().path("post/admin/{id}").resolveTemplate("id", model.getId()).request().header(SecurityFilter.SECURITY_HEADER, token).delete().getStatus());
                for (int i = 0; i < 5; i++) {
                    try {
                        assertNull(entityManager.find(Post.class, model.getId()));
                    } catch (final AssertionError ae) {
                        if (i == 4) {
                            throw ae;
                        }
                        try {
                            sleep(500);
                        } catch (final InterruptedException e) {
                            Thread.interrupted();
                            fail(e.getMessage());
                        }
                    }
                }

                // categories are not deleted with rows
                assertNotNull(entityManager.createQuery("select c from Category c where c.name = :name", Category.class).setParameter("name", "New Category").getSingleResult());
            }
        });
    }

    @Test
    public void invalidNotification() {
        blog.withTempUser((user, token) -> {
            final Date published = new Date();
            final Response response = blog.target().path("post/admin").request()
                    .header(SecurityFilter.SECURITY_HEADER, token)
                    .post(Entity.entity(new PostModel(
                            0, "post", null, "Title", "Summ", "Content", null, null, published,
                            new UserModel(user, null, null, null, null, 0), 0, null, null,
                            new NotificationModel("Super new post on the blog\n\n" +
                                    ">>>> http://bit.ly/GDBEHIKBD3748 <<<<\n\n" +
                                    "About a super topic\n\n#hash #tag #super #yeah\n\n" +
                                    "Come and read more about it ASAP!\n", published)), MediaType.APPLICATION_JSON_TYPE));
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            assertEquals("Message too long: 143 instead of 140", response.readEntity(String.class));
        });
    }

    @Test
    public void adminFindById() {
        {
            final Post ref = createPostWithPublishedDate(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(1));
            blog.withTempUser((user, token) -> { // published
                assertPost(
                    ref,
                    blog.target().path("post/admin/{id}")
                        .resolveTemplate("id", ref.getId()).request()
                        .header(SecurityFilter.SECURITY_HEADER, token)
                        .accept(MediaType.APPLICATION_JSON_TYPE)
                        .get(PostModel.class));
            });
        }
        resetDB();
        {
            final Post ref = createPostWithPublishedDate(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1));
            blog.withTempUser((user, token) -> { // not published
                assertPost(
                    ref,
                    blog.target().path("post/admin/{id}")
                        .resolveTemplate("id", ref.getId()).request()
                        .header(SecurityFilter.SECURITY_HEADER, token)
                        .accept(MediaType.APPLICATION_JSON_TYPE)
                        .get(PostModel.class));
            });
        }
    }

    @Test
    public void findById() {
        final Post ref = createPostWithPublishedDate(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(1));
        assertPost(
            ref,
            blog.target().path("post/{id}")
                .resolveTemplate("id", ref.getId()).request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get(PostModel.class));
    }

    @Test(expected = NotFoundException.class)
    public void findBySlugNotPublished() {
        createPostWithPublishedDate(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1));
        blog.target().path("post/slug/{slug}").resolveTemplate("slug", "new-post").request()
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .get(PostModel.class);
    }

    @Test(expected = NotFoundException.class)
    public void findByIdNotPublished() {
        final Post ref = createPostWithPublishedDate(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1));
        blog.target().path("post/{id}").resolveTemplate("id", ref.getId()).request()
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .get(PostModel.class);
    }

    private void assertPost(final Post expected, final PostModel actual) {
        assertNotNull(expected.getTitle());  // sanity check: ensure ref is not corrupted or null and we don't check anything

        assertEquals(expected.getTitle(), actual.getTitle());
        assertEquals(expected.getSlug(), actual.getSlug());
        assertEquals(expected.getId(), actual.getId());
        assertNotNull(actual.getCreated());
        assertNotNull(actual.getPublished());
        assertNotNull(actual.getUpdated());
        assertEquals(expected.getAuthor().getDisplayName(), actual.getAuthor().getDisplayName());
    }
}
