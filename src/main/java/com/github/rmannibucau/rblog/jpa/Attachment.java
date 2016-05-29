package com.github.rmannibucau.rblog.jpa;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@Getter
@Setter
@NamedQueries({
    @NamedQuery(name = "Attachment.findByPost", query = "select a from Attachment a where a.post.id = :postId order by a.updated desc"),
    @NamedQuery(name = "Attachment.countByPost", query = "select count(a) from Attachment a where a.post.id = :postId")
})
@Table(name = "rblog_attachment")
public class Attachment extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post;

    @Lob
    private byte[] content;
}
