package com.github.rmannibucau.rblog.jpa;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "rblog_token")
@NamedQueries({
    @NamedQuery(name = "Token.deleteExpiredTokens", query = "delete from Token t where t.lastUsage < :date")
})
public class Token {
    @Id
    @Column(length = 128)
    private String value;

    @ManyToOne
    private User user;

    @Temporal(TemporalType.TIMESTAMP)
    private Date lastUsage;

    @PrePersist
    private void prePersist() {
        lastUsage = new Date();
    }

    @Override
    public int hashCode() {
        return getValue() == null ? super.hashCode() : getValue().hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return getValue() == null ? obj == this : (Token.class.isInstance(obj) && Token.class.cast(obj).getValue().equals(getValue()));
    }
}
