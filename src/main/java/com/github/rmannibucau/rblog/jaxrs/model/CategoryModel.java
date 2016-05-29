package com.github.rmannibucau.rblog.jaxrs.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryModel {
    private long id;
    private String name;
    private String slug;
    private String color;
    private CategoryModel parent;
    private Collection<CategoryModel> children;
    private long version;
}
