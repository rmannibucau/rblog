package com.github.rmannibucau.rblog.jaxrs.async;

import com.github.rmannibucau.rblog.configuration.Configuration;
import com.github.rmannibucau.rblog.event.DoBackup;
import lombok.RequiredArgsConstructor;

import javax.annotation.Priority;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.transaction.Transactional;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.Response;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Async
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_AFTER) // after security
public class AsyncInterceptor {
    @Inject
    private Metas metas;

    @Inject
    @Configuration("${rblog.jaxrs.timeout:10000}")
    private Integer jaxrsTimeout;

    @AroundInvoke
    public Object async(final InvocationContext ctx) throws Exception {
        final Meta m = metas.find(ctx.getMethod());
        final Object[] args = ctx.getParameters();

        final AsyncResponse response = AsyncResponse.class.cast(args[m.responseIndex]);
        response.setTimeout(jaxrsTimeout, MILLISECONDS);

        final NoContentHandlerAsyncResponse internalAsyncResponse = new NoContentHandlerAsyncResponse(response);
        args[m.responseIndex] = internalAsyncResponse;

        m.invoker.accept(response, () -> {
            try {
                ctx.proceed(); // do the invocation
                if (!internalAsyncResponse.isResumed()) { // use HTTP 204 instead of HTTP 200 if user didn't set a payload
                    response.resume(Response.noContent().build());
                }
            } catch (final WebApplicationException wae) { // let it go through JAXRS layer to be handled properly
                throw wae;
            } catch (final Exception e) { // unlikely since we catch it in JAXRS wrapper
                throw new IllegalStateException(e);
            }
        });
        return null; // method should return void by spec + we enforced it in metas.find(m)
    }


    @ApplicationScoped
    static class Metas { // cache to avoid to depend on the intercepted scope
        @Inject
        private BeanManager beanManager;

        @Resource(name = "thread/taskExecutor")
        private ManagedExecutorService executorService;

        @Inject
        private TransactionProvider transactionProvider;

        @Inject
        private Event<DoBackup> backup;

        private final ConcurrentMap<Method, Meta> metas = new ConcurrentHashMap<>();

        Meta find(final Method m) {
            return metas.computeIfAbsent(m, mtd -> {
                if (void.class != mtd.getReturnType()) {
                    throw new IllegalArgumentException("@Suspended methods should return void");
                }

                // we could use Annotated type but we don't need it there (= this app)
                final AnnotatedType<?> type = beanManager.createAnnotatedType(mtd.getDeclaringClass());
                final Async async = ofNullable(
                        type.getMethods().stream()
                                .filter(am -> mtd.equals(am.getJavaMember()))
                                .findFirst()
                                .map(am -> am.getAnnotation(Async.class))
                                .orElseGet(() -> type.getAnnotation(Async.class)))
                        .orElseThrow(() -> new IllegalArgumentException("No @Async on " + mtd));


                final Parameter[] parameters = mtd.getParameters();
                for (int i = 0; i < parameters.length; i++) {
                    if (parameters[i].isAnnotationPresent(Suspended.class)) {
                        final boolean doBackup = async.backup();
                        final BiConsumer<AsyncResponse, Runnable> invoker = async.transactional() ? this::runInTransaction : this::run;
                        return new Meta((a, r) -> {
                            invoker.accept(a, r);
                            if (doBackup) { // after tx is commited
                                backup.fire(new DoBackup());
                            }
                        }, i);
                    }
                }
                throw new IllegalArgumentException("No @Suspended paramter for " + mtd);
            });
        }

        private void run(final AsyncResponse response, final Runnable task) {
            executorService.submit(toJaxRs(response, task));
        }

        private void runInTransaction(final AsyncResponse response, final Runnable task) {
            run(response, () -> transactionProvider.run(task));
        }

        private Runnable toJaxRs(final AsyncResponse response, final Runnable task) {
            return () -> { // ensure WebApplicationException are propagated
                try {
                    task.run();
                } catch (final Throwable e) {
                    response.resume(e);
                }
            };
        }
    }

    @ApplicationScoped
    static class TransactionProvider {
        @Transactional
        void run(final Runnable runnable) {
            runnable.run();
        }
    }

    @RequiredArgsConstructor
    private static class Meta {
        private final BiConsumer<AsyncResponse, Runnable> invoker;
        private final int responseIndex;
    }

    @RequiredArgsConstructor
    private static class NoContentHandlerAsyncResponse implements AsyncResponse {
        private final AsyncResponse delegate;
        private volatile boolean resumed;

        public boolean isResumed() {
            return resumed;
        }

        @Override
        public boolean resume(final Object response) {
            this.resumed = true;
            return delegate.resume(response);
        }

        @Override
        public boolean resume(final Throwable response) {
            return delegate.resume(response);
        }

        @Override
        public boolean cancel() {
            return delegate.cancel();
        }

        @Override
        public boolean cancel(final int retryAfter) {
            return delegate.cancel(retryAfter);
        }

        @Override
        public boolean cancel(final Date retryAfter) {
            return delegate.cancel(retryAfter);
        }

        @Override
        public boolean isSuspended() {
            return delegate.isSuspended();
        }

        @Override
        public boolean isCancelled() {
            return delegate.isCancelled();
        }

        @Override
        public boolean isDone() {
            return delegate.isDone();
        }

        @Override
        public boolean setTimeout(final long time, final TimeUnit unit) {
            return delegate.setTimeout(time, unit);
        }

        @Override
        public void setTimeoutHandler(final TimeoutHandler handler) {
            delegate.setTimeoutHandler(handler);
        }

        @Override
        public Collection<Class<?>> register(final Class<?> callback) {
            return delegate.register(callback);
        }

        @Override
        public Map<Class<?>, Collection<Class<?>>> register(final Class<?> callback, final Class<?>... callbacks) {
            return delegate.register(callback, callbacks);
        }

        @Override
        public Collection<Class<?>> register(final Object callback) {
            return delegate.register(callback);
        }

        @Override
        public Map<Class<?>, Collection<Class<?>>> register(final Object callback, final Object... callbacks) {
            return delegate.register(callback, callbacks);
        }
    }
}
