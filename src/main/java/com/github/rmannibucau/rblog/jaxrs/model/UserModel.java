package com.github.rmannibucau.rblog.jaxrs.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserModel {
    private long id;
    private String username;
    private String displayName;
    private String mail;
    private String password;
    private long version;
}
