package com.github.rmannibucau.rblog.jaxrs.reflect;

import lombok.RequiredArgsConstructor;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@RequiredArgsConstructor
public class CollectionType implements ParameterizedType {
    private final Type raw;
    private final Type[] args;

    @Override
    public Type[] getActualTypeArguments() {
        return args;
    }

    @Override
    public Type getRawType() {
        return raw;
    }

    @Override
    public Type getOwnerType() {
        return null;
    }
}
