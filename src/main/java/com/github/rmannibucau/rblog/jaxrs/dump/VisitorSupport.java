package com.github.rmannibucau.rblog.jaxrs.dump;

import com.github.rmannibucau.rblog.configuration.Configuration;
import com.github.rmannibucau.rblog.jpa.Post;
import lombok.Getter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.util.Date;
import java.util.function.Consumer;
import java.util.stream.IntStream;

@ApplicationScoped
public class VisitorSupport {
    @Inject
    private EntityManager entityManager;

    @Inject
    @Configuration("${rblog.visitor.pageSize:20}")
    private Integer pageSize;

    @Inject
    @Getter
    @Configuration("rblog.visitor.base")
    private String base;

    public String buildLink(final Post post) {
        return base + "#!/post/" + post.getSlug();
    }

    public void visit(final Consumer<Post> consumer) {
        final Date now = new Date();
        try {
            final long total = entityManager.createNamedQuery(Post.COUNT_ALL_PUBLISHED, Number.class)
                    .setParameter("date", now)
                    .getSingleResult().longValue();

            IntStream.range(0, (int) Math.ceil(total * 1. / pageSize))
                    .mapToObj(i -> i) // to prepare the next flatMap
                    .flatMap(page -> entityManager.createNamedQuery(Post.FIND_ALL_PUBLISHED, Post.class)
                            .setParameter("date", now)
                            .setFirstResult(page * pageSize)
                            .setMaxResults(pageSize).getResultList()
                            .stream())
                    .forEach(consumer);
        } catch (final NoResultException nre) {
            // ok, means no url
        }
    }
}
