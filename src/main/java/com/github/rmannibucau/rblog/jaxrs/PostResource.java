package com.github.rmannibucau.rblog.jaxrs;

import com.github.rmannibucau.rblog.configuration.Configuration;
import com.github.rmannibucau.rblog.jaxrs.async.Async;
import com.github.rmannibucau.rblog.jaxrs.model.AttachmentModel;
import com.github.rmannibucau.rblog.jaxrs.model.CategoryModel;
import com.github.rmannibucau.rblog.jaxrs.model.Message;
import com.github.rmannibucau.rblog.jaxrs.model.NotificationModel;
import com.github.rmannibucau.rblog.jaxrs.model.PostModel;
import com.github.rmannibucau.rblog.jaxrs.model.PostPage;
import com.github.rmannibucau.rblog.jaxrs.model.TopPosts;
import com.github.rmannibucau.rblog.jaxrs.model.UserModel;
import com.github.rmannibucau.rblog.jaxrs.provider.EntityConcurrentModificationException;
import com.github.rmannibucau.rblog.jpa.Attachment;
import com.github.rmannibucau.rblog.jpa.Category;
import com.github.rmannibucau.rblog.jpa.Notification;
import com.github.rmannibucau.rblog.jpa.Post;
import com.github.rmannibucau.rblog.jpa.PostType;
import com.github.rmannibucau.rblog.jpa.User;
import com.github.rmannibucau.rblog.security.cdi.Logged;
import com.github.rmannibucau.rblog.service.SlugService;
import com.github.rmannibucau.rblog.social.SocialNotifier;

import javax.ejb.EJBException;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
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
import javax.ws.rs.core.SecurityContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;

import static com.github.rmannibucau.rblog.lang.Exceptions.orNull;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.persistence.TemporalType.TIMESTAMP;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

@Path("post")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class PostResource {
    private static final Collection<String> POSSIBLE_ORDER_BY = new HashSet<>(asList(
            "none", "id", "title", "slug", "type", "created", "updated", "publishDate", "author.displayName"));
    private static final Collection<String> POSSIBLE_ORDER = new HashSet<>(asList("asc", "desc", "none"));
    private static final Collection<String> POSSIBLE_STATUS = new HashSet<>(asList("unpublished", "published"));
    private static final String[] SEARCH_FIELDS = {"title", "summary", "content"};

    @Inject
    private EntityManager entityManager;

    @Inject
    private SlugService slugService;

    @Inject
    private CategoryResource categoryResource;

    @Inject
    @Configuration("${rblog.pagination.max:100}")
    private Integer maxLimit;

    @Inject
    @Configuration("${rblog.categories.autoCreate:true}")
    private Boolean autoCreateCategories;

    @Inject
    @Configuration("${rblog.posts.top.size:3}")
    private Integer topSize;

    @Inject
    private SocialNotifier notfier;

    @GET
    @Async
    @Path("top")
    public void getTop(@Suspended final AsyncResponse response) { // TODO: cache by hour and eviction on notification?
        final TopPosts top = new TopPosts();
        final Date now = new Date();
        top.setLasts(
                entityManager.createNamedQuery(Post.FIND_ALL_PUBLISHED, Post.class)
                        .setParameter("date", now)
                        .setMaxResults(topSize)
                        .getResultList().stream()
                        .map(p -> map(p, true))
                        .collect(toList()));
        top.setByCategories( // only root categories
                entityManager.createNamedQuery("Category.findByParent", Category.class)
                        .setParameter("parent", null)
                        .getResultList().stream()
                        .collect(toMap(
                                Category::getName,
                                c -> new TopPosts.CategoryData(
                                        entityManager.createNamedQuery(Post.FIND_ALL_PUBLISHED_BY_CATEGORY, Post.class)
                                                .setParameter("category", c)
                                                .setParameter("date", now)
                                                .setMaxResults(topSize)
                                                .getResultList().stream()
                                                .map(p -> map(p, true))
                                                .collect(toList()),
                                        c.getSlug(),
                                        c.getColor()),
                                (u, v) -> {
                                    throw new IllegalStateException(String.format("Duplicate key %s", u));
                                },
                                TreeMap::new)));
        response.resume(top);
    }

    @GET
    @Async
    @Path("select")
    public void getPages(@QueryParam("offset") @DefaultValue("0") final int offset,
                         @QueryParam("number") @DefaultValue("20") final int max,
                         @QueryParam("orderBy") @DefaultValue("publishDate") final String orderBy,
                         @QueryParam("order") @DefaultValue("desc") final String order,
                         @QueryParam("type") @DefaultValue("post") final String type,
                         @QueryParam("after") final String afterDate,
                         @QueryParam("before") final String beforeDate,
                         @QueryParam("status") final String status,
                         @QueryParam("search") final String search,
                         @QueryParam("categoryId") final long categoryId,
                         @QueryParam("categorySlug") final String categorySlug,
                         @Context final SecurityContext securityContext,
                         @Suspended final AsyncResponse response) {
        if (max > maxLimit) {
            throw new WebApplicationException(Response.status(Response.Status.PRECONDITION_FAILED).entity(new Message("max post is limited to " + maxLimit)).build());
        }

        if (!POSSIBLE_ORDER_BY.contains(orderBy)) {
            throw new WebApplicationException(Response.status(Response.Status.PRECONDITION_FAILED).entity(new Message("Invalid order_by: " + orderBy)).build());
        }
        if (!POSSIBLE_ORDER.contains(order)) {
            throw new WebApplicationException(Response.status(Response.Status.PRECONDITION_FAILED).entity(new Message("Invalid order: " + order)).build());
        }
        if (status != null && !POSSIBLE_STATUS.contains(status)) {
            throw new WebApplicationException(Response.status(Response.Status.PRECONDITION_FAILED).entity(new Message("Invalid status: " + status)).build());
        }

        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Post> query = builder.createQuery(Post.class);
        final CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
        final Root<Post> root = query.from(Post.class);
        final Root<Post> countRoot = countQuery.from(Post.class);

        final CriteriaQuery<Post> select = query.select(root);
        final CriteriaQuery<Long> countSelect = countQuery.select(builder.count(countRoot));
        final Collection<Predicate> predicates = new ArrayList<>();
        final Collection<Predicate> countPredicates = new ArrayList<>();

        if (categoryId > 0 || categorySlug != null) { // select p from Post p join  p.categories c where c.slug = :s
            final Join<Post, Category> categories = root.join("categories", JoinType.INNER);
            final Join<Post, Category> countCategories = countRoot.join("categories", JoinType.INNER);
            if (categoryId > 0) {
                predicates.add(builder.equal(categories.get("id"), categoryId));
                countPredicates.add(builder.equal(countCategories.get("id"), categoryId));
            }
            ofNullable(categorySlug).ifPresent(slug -> {
                predicates.add(builder.like(categories.get("slug"), slug));
                countPredicates.add(builder.like(countCategories.get("slug"), slug));
            });
        }

        if (!"all".equals(type)) {
            final PostType postType = PostType.valueOf(type.toUpperCase(Locale.ENGLISH));
            predicates.add(builder.equal(root.get("type"), postType));
            countPredicates.add(builder.equal(countRoot.get("type"), postType));
        }
        ofNullable(afterDate).ifPresent(after -> {
            final Date asDate = parseDate(after);
            predicates.add(builder.greaterThanOrEqualTo(root.get("publishDate"), asDate));
            countPredicates.add(builder.greaterThanOrEqualTo(countRoot.get("publishDate"), asDate));
        });
        ofNullable(beforeDate).ifPresent(before -> {
            final Date asDate = parseDate(before);
            predicates.add(builder.lessThanOrEqualTo(root.get("publishDate"), asDate));
            countPredicates.add(builder.lessThanOrEqualTo(countRoot.get("publishDate"), asDate));
        });
        ofNullable(status == null && securityContext.getUserPrincipal() == null ? "published" : status).ifPresent(t -> {
            final Date now = new Date();
            switch (t) {
                case "published":
                    predicates.add(builder.lessThan(root.get("publishDate"), now));
                    countPredicates.add(builder.lessThan(countRoot.get("publishDate"), now));
                    break;
                case "unpublished":
                    predicates.add(builder.greaterThanOrEqualTo(root.get("publishDate"), now));
                    countPredicates.add(builder.greaterThanOrEqualTo(countRoot.get("publishDate"), now));
                    break;
                // other case not possible yet
            }
        });
        ofNullable(search).filter(s -> !s.trim().isEmpty()).ifPresent(s -> {
            // here we should use a better indexation solution:
            // - lucene
            // - elastic search
            // - MySQL/Oracle direct full text search (native query)
            //
            // for now keep it simple to use and install doing a contain on known fields

            final String[] text = s.toLowerCase(Locale.ENGLISH).split(" +");
            final BinaryOperator<Predicate> orCombiner = (predicate, predicate2) -> ofNullable(predicate).map(p -> builder.or(p, predicate2)).orElse(predicate2);

            predicates.add(
                    Stream.of(SEARCH_FIELDS)
                            .map(field ->
                                    Stream.of(text)
                                            .map(keyword -> builder.like(
                                                    builder.lower(root.get(field)),
                                                    '%' + keyword + '%'))
                                            .reduce(null, orCombiner))
                            .reduce(null, orCombiner));
            countPredicates.add(
                    Stream.of(SEARCH_FIELDS)
                            .map(field ->
                                    Stream.of(text)
                                            .map(keyword -> builder.like(
                                                    builder.lower(countRoot.get(field)),
                                                    '%' + keyword + '%'))
                                            .reduce(null, orCombiner))
                            .reduce(null, orCombiner));
        });
        if (!predicates.isEmpty()) {
            select.where(builder.and(predicates.stream().toArray(Predicate[]::new)));
            countSelect.where(builder.and(countPredicates.stream().toArray(Predicate[]::new)));
        }
        if (!"none".equals(orderBy)) {
            if ("author.displayName".equals(orderBy)) {
                final javax.persistence.criteria.Path<Object> path = root.get("author").get("displayName");
                select.orderBy("desc".equals(order) ? builder.desc(path) : builder.asc(path));
            } else { // simple field
                select.orderBy("desc".equals(order) ? builder.desc(root.get(orderBy)) : builder.asc(root.get(orderBy)));
            }
        }

        final List<PostModel> items = entityManager.createQuery(select)
                .setMaxResults(max).setFirstResult(offset).getResultList().stream()
                .map(p -> map(p, false))
                .collect(toList());

        long total;
        try {
            total = ofNullable(entityManager.createQuery(countQuery).getSingleResult()).orElse(0L);
        } catch (final NoResultException nre) {
            total = 0;
        }

        response.resume(new PostPage(total, items));
    }

    @POST
    @Logged
    @Async(backup = true)
    @Path("admin")
    @Consumes(MediaType.APPLICATION_JSON)
    public void post(final PostModel model, @Suspended final AsyncResponse response) {
        final boolean newEntry = model.getId() == 0;

        final NotificationModel notificationModel = model.getNotification();
        final boolean hasNotification = notificationModel != null && notificationModel.getText() != null && !notificationModel.getText().trim().isEmpty();
        if (hasNotification) {
            try {
                notfier.validate(notificationModel.getText());
            } catch (final EJBException e) {
                rethrowNotificationError(e.getCause());
            } catch (final IllegalArgumentException iae) {
                rethrowNotificationError(iae);
            }
        }

        final Post post;
        if (newEntry) {
            post = new Post();
        } else {
            post = entityManager.find(Post.class, model.getId());
            if (post.getVersion() != model.getVersion()) {
                throw new EntityConcurrentModificationException();
            }
        }

        post.setAuthor(entityManager.find(User.class, model.getAuthor().getId()));
        post.setTitle(model.getTitle());
        post.setSummary(model.getSummary());
        post.setContent(model.getContent());
        post.setPublishDate(ofNullable(model.getPublished()).orElseGet(Date::new));
        post.setSlug(ofNullable(model.getSlug()).filter(s -> !s.trim().isEmpty()).orElseGet(() -> slugService.slugFrom(model.getTitle())));
        post.setType(ofNullable(model.getType()).map(s -> s.toUpperCase(Locale.ENGLISH)).map(PostType::valueOf).orElse(PostType.POST));

        ofNullable(model.getCategories())
                .ifPresent(cats -> {
                    final List<Category> categories = cats.stream()
                            .map(c -> ofNullable(entityManager.find(Category.class, c.getId())).orElseGet(() -> {
                                if (autoCreateCategories) {
                                    return categoryResource.create(c);
                                }
                                throw new IllegalStateException("Missing category: " + c.getName());
                            }))
                            .collect(toList());

                    // sorting is not assured by the DB there
                    Collections.sort(categories, (o1, o2) -> o1.getName().compareTo(o2.getName()));

                    post.setCategories(categories);
                });

        if (newEntry) {
            entityManager.persist(post);
        }

        // notification
        Notification notification = entityManager.find(Notification.class, post.getId());
        if (hasNotification) {
            final Date date = ofNullable(notificationModel.getDate()).orElse(model.getPublished());

            final boolean newNotif = notification == null;
            if (newNotif) {
                notification = new Notification();
            }
            notification.setText(notificationModel.getText());
            notification.setDate(date);

            if (newNotif) {
                if (newEntry) { // ensure we have an id
                    entityManager.flush();
                }
                notification.setId(post.getId()); // link is done like that cause notification are not really in relationship with post but we want 1 notif/post max
                entityManager.persist(notification);
            }
        } else if (notification != null) {
            entityManager.remove(notification);
        }

        entityManager.flush();

        response.resume(findById(post.getId()));
    }

    @GET
    @Async
    @Logged
    @Path("admin/{id}")
    public void getByIdAdmin(@PathParam("id") final long id, @Suspended final AsyncResponse response) {
        response.resume(findById(id));
    }

    @DELETE
    @Async(backup = true)
    @Logged
    @Path("admin/{id}")
    public void deleteByIdAdmin(@PathParam("id") final long id, @Suspended final AsyncResponse response) {
        final Post post = entityManager.find(Post.class, id);
        ofNullable(post.getAttachments()).ifPresent(a -> a.forEach(entityManager::remove));
        ofNullable(entityManager.find(Notification.class, id)).ifPresent(n -> entityManager.remove(n));
        entityManager.remove(post);
        entityManager.flush();
    }

    @GET
    @Async
    @Path("slug/{slug}")
    public void getBySlug(@PathParam("slug") final String slug, @Suspended final AsyncResponse response) {
        response.resume(
                map(orNull(
                        () -> entityManager.createNamedQuery(Post.FIND_BY_SLUG, Post.class)
                                .setParameter("slug", slug)
                                .setParameter("publishedDate", new Date(), TIMESTAMP)
                                .getSingleResult(),
                        NoResultException.class), false));
    }

    @GET
    @Async
    @Path("{id}")
    public void getById(@PathParam("id") final long id, @Suspended final AsyncResponse response) {
        response.resume(
                map(orNull(
                        () -> entityManager.createNamedQuery(Post.FIND_BY_ID, Post.class)
                                .setParameter("id", id)
                                .setParameter("publishedDate", new Date(), TIMESTAMP)
                                .getSingleResult(),
                        NoResultException.class), false));
    }

    private void rethrowNotificationError(final Throwable iae) {
        throw new WebApplicationException(Response.status(BAD_REQUEST).entity(iae.getMessage()).build());
    }

    private PostModel findById(final long id) {
        return map(entityManager.find(Post.class, id), false);
    }

    private Date parseDate(final String date) {
        return Date.from(Instant.parse(date));
    }

    private PostModel map(final Post by, final boolean light) {
        return ofNullable(by).map(p ->
                new PostModel(
                        p.getId(), ofNullable(by.getType()).orElse(PostType.POST).name(), p.getSlug(), p.getTitle(),
                        p.getSummary(), light ? null : p.getContent(),
                        ofNullable(p.getCreated()).map(d -> new Date(d.getTime())).orElse(null),
                        ofNullable(p.getUpdated()).map(d -> new Date(d.getTime())).orElse(null),
                        ofNullable(p.getPublishDate()).map(d -> new Date(d.getTime())).orElse(null),
                        ofNullable(p.getAuthor()).map(a -> new UserModel(a.getId(), a.getUsername(), a.getDisplayName(), a.getMail(), null, a.getVersion())).orElse(null),
                        p.getVersion(),
                        ofNullable(by.getCategories()).orElse(emptyList()).stream()
                                .filter(c -> c.getChildren() == null || c.getChildren().isEmpty()) // only leafs
                                .map(c -> new CategoryModel(c.getId(), c.getName(), c.getSlug(), c.getColor(), null, null, c.getVersion()))
                                .collect(toList()),
                        ofNullable(by.getAttachments()).orElse(emptySet()).stream()
                                .map(Attachment::getId)
                                .map(AttachmentModel::new)
                                .collect(toList()),
                        ofNullable(entityManager.find(Notification.class, by.getId()))
                                .map(n -> new NotificationModel(n.getText(), ofNullable(n.getDate()).map(d -> new Date(d.getTime())).orElse(null)))
                                .orElse(null)))
                .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
    }
}
