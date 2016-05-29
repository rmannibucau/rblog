package com.github.rmannibucau.rblog.jaxrs.model;

import com.github.rmannibucau.rblog.jaxrs.format.JSDateConverter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.johnzon.mapper.JohnzonConverter;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostModel {
    private long id;
    private String type;
    private String slug;
    private String title;
    private String summary;
    private String content;

    @JohnzonConverter(JSDateConverter.class)
    private Date created;

    @JohnzonConverter(JSDateConverter.class)
    private Date updated;

    @JohnzonConverter(JSDateConverter.class)
    private Date published;

    private UserModel author;
    private long version;
    private List<CategoryModel> categories;
    private List<AttachmentModel> attachments;
    private NotificationModel notification;
}
