package com.github.rmannibucau.rblog.jaxrs;

import static java.util.Collections.emptySet;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import java.lang.reflect.Type;
import java.util.List;

import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheResult;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.github.rmannibucau.rblog.configuration.Configuration;
import com.github.rmannibucau.rblog.jaxrs.async.Async;
import com.github.rmannibucau.rblog.jaxrs.model.CategoryModel;
import com.github.rmannibucau.rblog.jaxrs.provider.EntityConcurrentModificationException;
import com.github.rmannibucau.rblog.jaxrs.reflect.CollectionType;
import com.github.rmannibucau.rblog.jcache.LocalCacheManager;
import com.github.rmannibucau.rblog.jpa.Category;
import com.github.rmannibucau.rblog.security.cdi.Logged;
import com.github.rmannibucau.rblog.service.SlugService;

@Path("category")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class CategoryResource {
    private static final Type COLLECTION = new CollectionType(List.class, new Type[]{CategoryModel.class});

    @Inject
    private EntityManager entityManager;

    @Inject
    private SlugService slugService;

    @Inject
    @Configuration("${rblog.category.defaultColor:#000000}")
    private String defaultColor;

    @Inject
    private LocalCacheManager cache;

    @GET
    @Path("roots")
    @Transactional
    @CacheResult(cacheResolverFactory = LocalCacheManager.class, nonCachedExceptions = Exception.class)
    public List<CategoryModel> getParents() {
        return entityManager.createNamedQuery("Category.findByParent", Category.class)
                        .setParameter("parent", null)
                        .getResultList().stream()
                        .map(c -> toModel(c, true))
                        .collect(toList());
    }

    @GET
    @Path("all")
    @Transactional
    @CacheResult(cacheResolverFactory = LocalCacheManager.class, nonCachedExceptions = Exception.class)
    public List<CategoryModel> getAll() {
        return entityManager.createNamedQuery("Category.findAll", Category.class)
                        .getResultList().stream()
                        .map(c -> toModel(c, false))
                        .collect(toList());
    }

    @GET
    @Path("{id}")
    @Transactional
    @CacheResult(cacheResolverFactory = LocalCacheManager.class, nonCachedExceptions = Exception.class)
    public CategoryModel get(@CacheKey @PathParam("id") final long id) {
        return findById(id);
    }

    @GET
    @Path("slug/{slug}")
    @Transactional
    @CacheResult(cacheResolverFactory = LocalCacheManager.class, nonCachedExceptions = Exception.class)
    public CategoryModel get(@PathParam("slug") final String slug) {
        return ofNullable(entityManager.createNamedQuery("Category.findBySlug", Category.class)
                .setParameter("slug", slug)
                .getSingleResult())
                .map(c -> toModel(c, true))
                .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
    }

    @POST
    @Async(backup = true)
    @Logged
    @Consumes(MediaType.APPLICATION_JSON)
    public void post(final CategoryModel model, @Suspended final AsyncResponse response) {
        final CategoryModel byId = findById(create(model).getId());
        response.resume(byId);
    }

    @Async(backup = true)
    @DELETE
    @Logged
    @Path("{id}")
    public void remove(@PathParam("id") final long id, @Suspended final AsyncResponse response) {
        final Category entity = entityManager.find(Category.class, id);
        if (entity.getChildren() != null) {
            entity.getChildren().forEach(c -> c.setParent(null));
        }
        entityManager.remove(entity);
        entityManager.flush();
        cache.invalidate(CategoryResource.class);
    }

    private CategoryModel findById(final long id) {
        return ofNullable(entityManager.find(Category.class, id))
                .map(c -> toModel(c, true))
                .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
    }

    // @Transactional @Logged: supposed
    Category create(final CategoryModel model) {
        final boolean newEntry = model.getId() == 0;

        final Category category;
        if (newEntry) {
            category = new Category();
        } else {
            category = entityManager.find(Category.class, model.getId());
            if (category.getVersion() != model.getVersion()) {
                throw new EntityConcurrentModificationException();
            }
        }

        category.setName(model.getName());
        category.setSlug(ofNullable(model.getSlug()).filter(s -> !s.trim().isEmpty()).orElseGet(() -> slugService.slugFrom(model.getName())));
        category.setParent(
                ofNullable(model.getParent())
                        .map(p -> entityManager.find(Category.class, p.getId()))
                        .filter(c -> !category.equals(c)) // avoid loops
                        .orElse(null));
        category.setColor(ofNullable(model.getColor()).orElse(defaultColor));

        if (newEntry) {
            entityManager.persist(category);
        }
        entityManager.flush();
        cache.invalidate(CategoryResource.class);
        return category;
    }

    private CategoryModel toModel(final Category cat, final boolean linkChildren) {
        return new CategoryModel(
                cat.getId(), cat.getName(), cat.getSlug(), ofNullable(cat.getColor()).orElse(defaultColor),
                ofNullable(cat.getParent()).map(c -> toModel(c, false)).orElse(null),
                linkChildren ? ofNullable(cat.getChildren()).orElse(emptySet()).stream().map(c -> toModel(c, true)).collect(toList()) : null,
                cat.getVersion());
    }
}
