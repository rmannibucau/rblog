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
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RBlog.Runner.class)
public class SiteMapResourceTest {
    @Application
    private RBlog blog;

    @Inject
    private EntityManager entityManager;

    @Inject
    private SiteMapResource siteMapResource;

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
                post.setContent("some content");
                post.setPublishDate(new Date(System.currentTimeMillis() + (i % 3 == 0 ? -1 : 1) * TimeUnit.MINUTES.toMillis(30)));
                entityManager.persist(post);
            });
        });

        // force to regenerate if relevant
        siteMapResource.doGenerate().get();
    }

    @After
    public void clean() {
        blog.clean();
    }

    @Test
    public void generate() {
        final SiteMapResource.Urlset sitemap = blog.target().path("sitemap").request().get(SiteMapResource.Urlset.class);
        assertEquals(4, sitemap.getUrl().size());

        sitemap.getUrl().forEach(url -> {
            assertEquals(SiteMapResource.TChangeFreq.WEEKLY, url.getChangefreq());

            assertNotNull(url.getLastmod());
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(url.getLastmod()); // to ensure it is parsable

            assertEquals(0.5, url.getPriority(), 0.);

            assertTrue(url.getLoc().substring(0, url.getLoc().length() - 1).endsWith("/post/new-post-"));
        });
    }
}
