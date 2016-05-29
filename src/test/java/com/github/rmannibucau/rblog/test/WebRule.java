package com.github.rmannibucau.rblog.test;

import lombok.RequiredArgsConstructor;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.openqa.selenium.WebDriver;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.fail;

@RequiredArgsConstructor
public class WebRule implements TestRule {
    private final Object test;

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                final RBlog blog = findBlogInstance();
                blog.injectWeb(test);
                Stream.of(test.getClass().getDeclaredFields())
                    .filter(f -> f.isAnnotationPresent(Web.class) && f.getType() == WebDriver.class)
                    .forEach(f -> {
                        f.setAccessible(true);
                        try {
                            f.set(test, blog.browser());
                        } catch (final IllegalAccessException e) {
                            fail(e.getMessage());
                        }
                    });
                base.evaluate();
            }
        };
    }

    private RBlog findBlogInstance() throws NoSuchFieldException, IllegalAccessException {
        final Field app = RBlog.Runner.class.getDeclaredField("APP");
        if (!app.isAccessible()) {
            app.setAccessible(true);
        }
        return RBlog.class.cast(AtomicReference.class.cast(app.get(null)).get());
    }

    @Target({ FIELD, TYPE })
    @Retention(RUNTIME)
    public @interface Web {
    }
}
