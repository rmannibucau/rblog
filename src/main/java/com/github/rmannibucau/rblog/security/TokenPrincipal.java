package com.github.rmannibucau.rblog.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.security.Principal;

@Getter
@AllArgsConstructor
public class TokenPrincipal implements Principal {
    private final String name;
    private final String displayName;
    private final String token;
}
