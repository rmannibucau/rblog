package com.github.rmannibucau.rblog.jaxrs;

import com.github.rmannibucau.rblog.jaxrs.async.Async;
import com.github.rmannibucau.rblog.jaxrs.model.UserModel;
import com.github.rmannibucau.rblog.jaxrs.provider.EntityConcurrentModificationException;
import com.github.rmannibucau.rblog.jaxrs.reflect.CollectionType;
import com.github.rmannibucau.rblog.jpa.User;
import com.github.rmannibucau.rblog.security.cdi.Logged;
import com.github.rmannibucau.rblog.service.PasswordService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.reflect.Type;
import java.util.List;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

@Logged
@Path("user")
@ApplicationScoped
public class UserResource {
    private static final CollectionType COLLECTION = new CollectionType(List.class, new Type[]{UserModel.class});

    @Inject
    private EntityManager entityManager;

    @Inject
    private PasswordService passwordService;

    @GET
    @Async(transactional = false)
    public void getAll(@Suspended final AsyncResponse response) {
        response.resume(
                new GenericEntity<>(
                        entityManager.createNamedQuery(User.FIND_ALL, User.class).getResultList().stream()
                                .map(u -> new UserModel(u.getId(), u.getUsername(), u.getDisplayName(), u.getMail(), null, u.getVersion()))
                                .collect(toList()),
                        COLLECTION));
    }

    @GET
    @Path("{id}")
    @Async(transactional = false)
    public void get(@PathParam("id") final long id, @Suspended final AsyncResponse response) {
        response.resume(findById(id));
    }

    @POST
    @Async(backup = true)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void post(final UserModel model, @Suspended final AsyncResponse response) {
        final User user;
        if (model.getId() == 0) {
            user = new User();
        } else {
            user = entityManager.find(User.class, model.getId());
            if (user.getVersion() != model.getVersion()) {
                throw new EntityConcurrentModificationException();
            }
        }

        if (user.getPassword() != null || model.getId() == 0) {
            user.setPassword(passwordService.toDatabaseFormat(model.getPassword()));
        }

        user.setUsername(model.getUsername());
        user.setDisplayName(model.getDisplayName());
        user.setMail(model.getMail());

        if (user.getDisplayName() == null) {
            user.setDisplayName(model.getUsername());
        }

        if (model.getId() == 0) {
            entityManager.persist(user);
        }
        entityManager.flush();

        response.resume(findById(user.getId()));
    }

    @DELETE
    @Async(backup = true)
    @Path("{id}")
    public void remove(@PathParam("id") final long id, @Suspended final AsyncResponse response) {
        entityManager.remove(entityManager.getReference(User.class, id));
        entityManager.flush();
    }

    private UserModel findById(final long id) {
        return ofNullable(entityManager.find(User.class, id))
                .map(u -> new UserModel(u.getId(), u.getUsername(), u.getDisplayName(), u.getMail(), null, u.getVersion()))
                .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
    }
}
