package com.github.rmannibucau.rblog.jpa;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Temporal;
import javax.persistence.Version;
import java.util.Date;

import static javax.persistence.TemporalType.TIMESTAMP;

@Getter
@Setter
@MappedSuperclass
public class BaseEntity {
    @Id
    @GeneratedValue
    private long id;

    @Version
    private long version;

    @Temporal(TIMESTAMP)
    private Date created;

    @Temporal(TIMESTAMP)
    private Date updated;

    @PrePersist
    private void onPersist() {
        created = updated = new Date();
    }

    @PreUpdate
    private void onUpdate() {
        updated = new Date();
    }

    @Override
    public int hashCode() {
        return id == 0 ? super.hashCode() : Long.hashCode(id);
    }

    @Override
    public boolean equals(final Object obj) {
        return getId() == 0 ? obj == this : (getClass().isInstance(obj) && BaseEntity.class.cast(obj).getId() == getId());
    }
}
