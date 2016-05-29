package com.github.rmannibucau.rblog.social;

import java.util.concurrent.CompletableFuture;

public interface SocialService {
    boolean isActive();

    CompletableFuture<?> publish(String message);
}
