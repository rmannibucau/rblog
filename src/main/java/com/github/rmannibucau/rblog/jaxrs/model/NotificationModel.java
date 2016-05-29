package com.github.rmannibucau.rblog.jaxrs.model;

import com.github.rmannibucau.rblog.jaxrs.format.JSDateConverter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.johnzon.mapper.JohnzonConverter;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationModel {
    private String text;

    @JohnzonConverter(JSDateConverter.class)
    private Date date;
}
