package com.github.rmannibucau.rblog.jpa;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.util.Set;

@Entity
@Getter
@Setter
@NamedQueries({
    @NamedQuery(name = User.FIND_BY_CREDENTIALS, query = "select u from User u where u.username = :username and u.password = :password"),
    @NamedQuery(name = User.FIND_ALL, query = "select u from User u order by u.displayName")
})
@Table(name = "rblog_user")
public class User extends BaseEntity {
    public static final String FIND_BY_CREDENTIALS = "User.findByUsernameAndPassword";
    public static final String FIND_ALL = "User.findAll";

    @Column(length = 160, unique = true, nullable = false)
    private String username;

    @Column(length = 512)
    private String displayName;

    @Column(length = 512)
    private String mail;

    @Column(length = 2048, nullable = false)
    private String password;

    @OneToMany(mappedBy = "user")
    private Set<Token> tokens;

    @PrePersist
    @PreUpdate
    public void ensureDisplayName() {
        if (displayName == null) {
            displayName = username;
        }
    }
}
