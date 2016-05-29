package com.github.rmannibucau.rblog.jpa;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

import static javax.persistence.EnumType.STRING;
import static javax.persistence.FetchType.EAGER;
import static javax.persistence.TemporalType.TIMESTAMP;

@Getter
@Setter
@Entity
@Table(name = "rblog_post")
@NamedQueries({
    @NamedQuery(name = Post.COUNT_ALL_PUBLISHED, query = "select count(p) from Post p where p.publishDate < :date"),
    @NamedQuery(name = Post.FIND_ALL_PUBLISHED, query = "select p from Post p where p.publishDate < :date order by p.publishDate desc"),
    @NamedQuery(name = Post.FIND_ALL_PUBLISHED_BY_CATEGORY, query = "select p from Post p where :category member of p.categories and p.publishDate < :date order by p.publishDate desc"),
    @NamedQuery(name = Post.FIND_BY_ID, query = "select p from Post p where p.id = :id and p.publishDate < :publishedDate"),
    @NamedQuery(name = Post.FIND_BY_SLUG, query = "select p from Post p where p.slug = :slug and p.publishDate < :publishedDate")
})
public class Post extends BaseEntity {
    // checking published date
    public static final String COUNT_ALL_PUBLISHED = "Post.countAll";
    public static final String FIND_ALL_PUBLISHED = "Post.findAll";
    public static final String FIND_BY_ID = "Post.findById";
    public static final String FIND_BY_SLUG = "Post.findBySlug";
    public static final String FIND_ALL_PUBLISHED_BY_CATEGORY = "Post.findAllByCategory";

    @Column(unique = true, nullable = false, length = 160)
    private String slug;

    @Enumerated(STRING)
    private PostType type;

    private String title;

    private String summary; // a length of 255 max is good enough for a teaser/overview

    @Lob
    private String content;

    @Temporal(TIMESTAMP)
    private Date publishDate;

    @ManyToOne
    @JoinColumn(name = "author_id")
    private User author;

    @ManyToMany
    @OrderBy("name asc")
    @JoinTable(
        name = "rblog_post_category",
        joinColumns = @JoinColumn(name = "post_id", referencedColumnName = "id"),
        inverseJoinColumns = @JoinColumn(name = "category_id", referencedColumnName = "id"))
    private Collection<Category> categories;

    @OneToMany(mappedBy = "post", fetch = EAGER)
    private Set<Attachment> attachments;
}
