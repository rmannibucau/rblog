package com.github.rmannibucau.rblog.social;

import java.util.concurrent.CompletableFuture;

public interface SocialService {
    boolean isActive();

    void validate(String message);

    CompletableFuture<?> publish(String message);
}
