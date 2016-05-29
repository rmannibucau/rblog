package com.github.rmannibucau.rblog.jaxrs.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttachmentPage {
    private long total;
    private List<AttachmentModel> attachments;
}
