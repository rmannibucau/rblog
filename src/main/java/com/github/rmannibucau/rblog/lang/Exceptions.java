package com.github.rmannibucau.rblog.lang;

import lombok.NoArgsConstructor;

import java.util.function.Supplier;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class Exceptions {
    public static  <T> T safe(final Supplier<T> supplier, final Supplier<T> onError, final Class<? extends Throwable> forException) {
        try {
            return supplier.get();
        } catch (final Throwable t) {
            if (forException.isInstance(t)) {
                return onError.get();
            }
            if (RuntimeException.class.isInstance(t)) {
                throw RuntimeException.class.cast(t);
            }
            throw new IllegalStateException(t);
        }
    }

    public static  <T> T orNull(final Supplier<T> supplier, final Class<? extends Throwable> forException) {
        return safe(supplier, () -> null, forException);
    }
}
