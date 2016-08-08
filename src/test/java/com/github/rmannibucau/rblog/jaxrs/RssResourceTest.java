package com.github.rmannibucau.rblog.jaxrs;

import com.github.rmannibucau.rblog.jpa.Post;
import com.github.rmannibucau.rblog.jpa.User;
import com.github.rmannibucau.rblog.test.RBlog;
import org.apache.openejb.testing.Application;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.ws.rs.core.MediaType;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RBlog.Runner.class)
public class RssResourceTest {
    @Application
    private RBlog blog;

    @Inject
    private EntityManager entityManager;

    @Inject
    private RssResource rssResource;

    private final AtomicLong aPostId = new AtomicLong();

    @After
    public void after() {
        blog.clean();
    }

    @Before
    public void data() throws ExecutionException, InterruptedException {
        blog.inTx(() -> {
            final User u = new User();
            u.setUsername("author");
            u.setPassword("whatever");
            entityManager.persist(u);

            IntStream.range(0, 10).forEach(i -> {
                final Post post = new Post();
                post.setAuthor(u);
                post.setSlug("new-post-" + i);
                post.setTitle("New post " + i);
                post.setSummary("Sum post " + i);
                post.setContent("some content");
                post.setPublishDate(new Date(System.currentTimeMillis() + (i % 3 == 0 ? -1 : 1) * TimeUnit.MINUTES.toMillis(30)));
                entityManager.persist(post);
                aPostId.set(post.getId());
            });
        });

        // force to regenerate and wait for the result for testing purposes
        rssResource.doGenerate().get();
    }

    @After
    public void clean() {
        blog.clean();
    }

    @Test
    public void generate() {
        final RssResource.Rss rss = blog.target().path("rss").request(MediaType.APPLICATION_XML).get(RssResource.Rss.class);
        assertEquals(4, rss.getChannel().getItem().size());
        rss.getChannel().getItem().forEach(this::assertPost);
    }

    @Test
    public void singlePost() {
        final RssResource.Rss rss = blog.target().path("rss").queryParam("post", aPostId.get()).request(MediaType.APPLICATION_XML).get(RssResource.Rss.class);
        assertEquals(1, rss.getChannel().getItem().size());
        assertPost(rss.getChannel().getItem().iterator().next());
    }

    private void assertPost(final RssResource.Item url) {
        assertNotNull(url.getPubDate());
        DateTimeFormatter.RFC_1123_DATE_TIME.parse(url.getPubDate());

        assertTrue(url.getDescription().startsWith("Sum post "));
        assertTrue(url.getTitle().startsWith("New post "));
        assertTrue(url.getLink().substring(0, url.getLink().length() - 1).endsWith("#/post/new-post-"));
    }
}
