package com.github.rmannibucau.rblog.jpa;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;
import java.util.Collection;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "rblog_category")
@NamedQueries({
    @NamedQuery(name = "Category.findByParent", query = "select c from Category c left join fetch c.children where c.parent = :parent order by c.name"),
    @NamedQuery(name = "Category.findBySlug", query = "select c from Category c where c.slug = :slug"),
    @NamedQuery(name = "Category.findAll", query = "select c from Category c order by c.name")
})
public class Category {
    @Id
    @GeneratedValue
    private long id;

    @Column(length = 160, unique = true)
    private String slug;

    @Column(length = 160, unique = true, nullable = false)
    private String name;

    @Column(length = 8)
    private String color;

    @ManyToMany(mappedBy = "categories")
    private Collection<Post> posts;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent")
    private Set<Category> children;

    @Version
    private long version;

    @Override
    public int hashCode() {
        return id == 0 ? super.hashCode() : Long.hashCode(id);
    }

    @Override
    public boolean equals(final Object obj) {
        return getId() == 0 ? obj == this : (getClass().isInstance(obj) && BaseEntity.class.cast(obj).getId() == getId());
    }
}
