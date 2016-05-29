package com.github.rmannibucau.rblog.jpa;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.Version;
import java.util.Date;

import static javax.persistence.EnumType.STRING;
import static javax.persistence.TemporalType.TIMESTAMP;

@Getter
@Setter
@Entity
@NamedQueries({
    @NamedQuery(name = "Notification.findByStateAndDate", query = "select n from Notification n where n.state = :state and n.date < :date")
})
@Table(name = "rblog_notification")
public class Notification {
    public enum State {
        TO_PUBLISH, PUBLISHING, PUBLISHED
    }

    @Id
    private long id; // postId actually

    @Enumerated(STRING)
    private State state;

    @Temporal(TIMESTAMP)
    @Column(nullable = false)
    private Date date;

    @Column(nullable = false)
    private String text;

    @Version
    private long version;

    @PrePersist
    @PreUpdate
    public void checkState() {
        final Date now = new Date();
        state = date.after(now) ? State.TO_PUBLISH : State.PUBLISHED /* weird but we don't want to republish it if due to an DB import */;
    }

    @Override
    public int hashCode() {
        return id == 0 ? super.hashCode() : Long.hashCode(id);
    }

    @Override
    public boolean equals(final Object obj) {
        return getId() == 0 ? obj == this : (Notification.class.isInstance(obj) && Notification.class.cast(obj).getId() == getId());
    }
}
