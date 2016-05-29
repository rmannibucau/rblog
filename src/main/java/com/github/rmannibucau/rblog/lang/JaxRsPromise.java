package com.github.rmannibucau.rblog.lang;

import lombok.RequiredArgsConstructor;

import javax.ws.rs.client.InvocationCallback;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class JaxRsPromise {
    // typed String cause of CXF-6793
    private CompletableFuture<String> delegate;

    public JaxRsPromise() {
        delegate = new CompletableFuture<>();
    }

    public JaxRsPromise propagateCancel(final Future<?> future) {
        delegate = delegate.whenComplete((result, exception) -> {  // try to cancel the call if completable future is cancelled
            if (CancellationException.class.isInstance(exception)) {
                future.cancel(true);
            }
        });
        return this;
    }

    public CompletableFuture<?> toFuture() {
        return delegate;
    }

    public InvocationCallback<?> toJaxRsCallback() {
        return new PromiseInvocationCallback(delegate);
    }

    @RequiredArgsConstructor
    private static class PromiseInvocationCallback implements InvocationCallback<String> {
        private final CompletableFuture<String> delegate;

        @Override
        public void completed(final String o) {
            delegate.complete(o);
        }

        @Override
        public void failed(final Throwable throwable) {
            delegate.completeExceptionally(throwable);
        }
    }
}
