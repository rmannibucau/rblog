package com.github.rmannibucau.rblog.social;

import com.github.rmannibucau.rblog.jpa.Notification;
import com.github.rmannibucau.rblog.test.RBlog;
import com.github.rmannibucau.rblog.test.TwitterMocks;
import org.apache.openejb.testing.Application;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(RBlog.Runner.class)
public class SocialNotifierTest {
    @Application
    private RBlog blog;

    @Inject
    private SocialNotifier notifier;

    @Inject
    private EntityManager entityManager;

    @Inject
    private TwitterMocks twitter;

    @Test
    public void send() { // we skip scheduling there but we believe in tomee ;)
        twitter.reset();

        blog.inTx(() -> {
            { // past
                final Notification notification = new Notification();
                notification.setId(12354);
                notification.setText("test #ok");
                notification.setDate(new Date(System.currentTimeMillis() - 7000));
                entityManager.persist(notification);
            }
            { // future
                final Notification notification = new Notification();
                notification.setId(12355);
                notification.setText("test #ok");
                notification.setDate(new Date(System.currentTimeMillis() + 5000));
                entityManager.persist(notification);
            }
        });

        // too fast (5s of delay are needed)
        flushNotifications();
        assertEquals(0, twitter.getValues().size());

        try {
            sleep(5000);
        } catch (final InterruptedException e) {
            Thread.interrupted();
            fail();
        }

        // we got the twitter notif
        flushNotifications();
        assertEquals("status=test%20%23ok", twitter.getValues().get("body"));

        // ensure we send it only once
        twitter.reset();
        flushNotifications();
        assertEquals(0, twitter.getValues().size());
    }

    private void flushNotifications() {
        notifier.publishIfNeeded();
        notifier.getTrackedTasks().forEach(f -> {
            try {
                f.get(1, TimeUnit.MINUTES);
            } catch (final InterruptedException ie) {
                Thread.interrupted();
                fail();
            } catch (final TimeoutException | ExecutionException e) {
                fail(e.getMessage());
            }
        });
    }
}
