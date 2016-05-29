package com.github.rmannibucau.rblog.jaxrs;

import com.github.rmannibucau.rblog.jaxrs.async.Async;
import com.github.rmannibucau.rblog.jaxrs.model.AttachmentModel;
import com.github.rmannibucau.rblog.jpa.Attachment;
import com.github.rmannibucau.rblog.jpa.Post;
import com.github.rmannibucau.rblog.security.cdi.Logged;
import com.github.rmannibucau.rblog.service.IOService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Path("attachment")
@ApplicationScoped
public class AttachmentResource {
    @Inject
    private EntityManager entityManager;

    @Inject
    private IOService io;

    @POST
    @Async
    @Logged
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public void upload(@Context final HttpServletRequest request,
                       @QueryParam("postId") final String postId,
                       @Suspended final AsyncResponse response) throws IOException, ServletException {
        final Post post = entityManager.find(Post.class, postId);
        if (post == null) { // eager check we find the post before loading the file content
            throw new WebApplicationException("Post not found.", Response.Status.BAD_REQUEST);
        }

        final Part part = request.getPart("file");
        if (part == null) {
            throw new WebApplicationException("No attachment found.", Response.Status.BAD_REQUEST);
        }

        switch (part.getContentType()) {
            case "image/png":
            case "image/gif":
            case "image/jpeg":
            case "image/jpg":
                final Attachment attachment = new Attachment();
                attachment.setPost(post);
                try (final InputStream in = part.getInputStream()) {
                    attachment.setContent(io.read(in));
                }
                entityManager.persist(attachment);
                entityManager.flush();
                response.resume(new AttachmentModel(attachment.getId()));
                return;

            default:
                throw new WebApplicationException("Unsupported content type.", Response.Status.BAD_REQUEST);
        }
    }

    @DELETE
    @Async
    @Logged
    @Path("{id}")
    public void delete(@PathParam("id") final long id, @Suspended final AsyncResponse response) {
        entityManager.remove(entityManager.find(Attachment.class, id));
        entityManager.flush();
    }

    @GET
    @Async
    @Path("{id}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public void get(@PathParam("id") final long id, @Suspended final AsyncResponse response) {
        final Attachment attachment = entityManager.find(Attachment.class, id);
        if (attachment == null) {
            throw new WebApplicationException("No attachment with id: " + id, Response.Status.NO_CONTENT);
        }
        response.resume(new ByteArrayInputStream(attachment.getContent()));
    }
}
