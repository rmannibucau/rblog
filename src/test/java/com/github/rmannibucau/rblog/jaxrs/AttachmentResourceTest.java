package com.github.rmannibucau.rblog.jaxrs;

import com.github.rmannibucau.rblog.jaxrs.model.AttachmentModel;
import com.github.rmannibucau.rblog.jpa.Post;
import com.github.rmannibucau.rblog.jpa.PostType;
import com.github.rmannibucau.rblog.security.web.SecurityFilter;
import com.github.rmannibucau.rblog.test.RBlog;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.InputStreamDataSource;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.openejb.testing.Application;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.net.ssl.HttpsURLConnection;
import javax.persistence.EntityManager;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RBlog.Runner.class)
public class AttachmentResourceTest {
    @Application
    private RBlog blog;

    @Inject
    private EntityManager em;

    private long postId;

    @Before
    public void before() {
        blog.inTx(() -> {
            final Post p = new Post();
            p.setType(PostType.POST);
            p.setSlug("slug");
            p.setTitle("title");
            p.setSummary("summary");
            p.setContent("content");
            em.persist(p);
            postId = p.getId();
        });
    }

    @After
    public void after() {
        blog.clean();
    }

    @Test
    public void upload() {
        blog.withAutoMultipartSupport(() -> blog.withTempUser((u, token) -> {
            final Attachment file = createAttachment();

            final AttachmentModel model = blog.target()
                .path("attachment").queryParam("postId", postId).request()
                .header(SecurityFilter.SECURITY_HEADER, token)
                .post(Entity.entity(file, MediaType.MULTIPART_FORM_DATA), AttachmentModel.class);

            assertTrue(model.getId() > 0);
            final com.github.rmannibucau.rblog.jpa.Attachment attachment = em.find(com.github.rmannibucau.rblog.jpa.Attachment.class, model.getId());
            assertEquals("fake-image", new String(attachment.getContent(), StandardCharsets.UTF_8));

            final Post post = em.find(Post.class, postId);
            assertEquals(1, post.getAttachments().size());
            assertEquals(model.getId(), post.getAttachments().iterator().next().getId());
        }));
    }

    @Test
    public void delete() {
        upload();
        blog.withTempUser((u, token) ->
            assertEquals(
                HttpsURLConnection.HTTP_NO_CONTENT,
                blog.target()
                    .path("attachment/{aId}").resolveTemplate("aId", attachementId()).request()
                    .header(SecurityFilter.SECURITY_HEADER, token)
                    .delete()
                    .getStatus()));
        assertTrue(em.find(Post.class, postId).getAttachments().isEmpty());
    }

    @Test
    public void get() {
        upload();
        assertEquals(
            "fake-image",
            blog.target()
                .path("attachment/{aId}").resolveTemplate("aId", attachementId()).request()
                .get(String.class));
    }

    @Test(expected = ForbiddenException.class)
    public void noCreateWithoutBeingLogged() {
        blog.target()
            .path("attachment").queryParam("postId", postId).request()
            .post(Entity.entity(createAttachment(), MediaType.MULTIPART_FORM_DATA), AttachmentModel.class);
    }

    @Test(expected = ForbiddenException.class)
    public void noDeleteWithoutBeingLogged() {
        upload();
        blog.target()
            .path("attachment/{aId}").resolveTemplate("aId", attachementId()).request()
            .delete(String.class);
    }

    private long attachementId() {
        return em.find(Post.class, postId).getAttachments().iterator().next().getId();
    }

    private Attachment createAttachment() {
        final MetadataMap<String, String> headers = new MetadataMap<>();
        headers.put("Content-Disposition", singletonList("form-data;name=file;filename=test.png"));
        return new Attachment(
            "file",
            new InputStreamDataSource(new ByteArrayInputStream("fake-image".getBytes(StandardCharsets.UTF_8)), "image/png"),
            headers);
    }
}
