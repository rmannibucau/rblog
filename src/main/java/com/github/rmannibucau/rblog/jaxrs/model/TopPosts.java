package com.github.rmannibucau.rblog.jaxrs.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Map;

@Data
public class TopPosts {
    private Collection<PostModel> lasts;
    private Map<String, CategoryData> byCategories;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryData {
        private Collection<PostModel> posts;
        private String slug;
        private String color;
        private long id;
    }
}
