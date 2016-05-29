package com.github.rmannibucau.rblog.social;

import com.github.rmannibucau.rblog.jpa.Notification;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.OptimisticLockException;
import javax.transaction.Transactional;
import javax.transaction.TransactionalException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionManagement(TransactionManagementType.BEAN)
public class SocialNotifier {
    @Any
    @Inject
    private Instance<SocialService> socialServices;

    @Inject
    private EntityManager entityManager;

    @Inject
    private NotifierStates states;

    private SocialService[] activeServices;
    private final Map<CompletableFuture<?>, Object> tracker = new ConcurrentHashMap<>();

    @PostConstruct
    private void filter() {
        activeServices = StreamSupport.stream(socialServices.spliterator(), false)
            .filter(SocialService::isActive)
            .toArray(SocialService[]::new);
    }

    @Schedule(hour = "*", minute = "*/2", persistent = false) // each 2 mn check if something to do
    public void publishIfNeeded() {
        final List<Notification> notifications = entityManager.createNamedQuery("Notification.findByStateAndDate", Notification.class)
            .setParameter("state", Notification.State.TO_PUBLISH)
            .setParameter("date", new Date())
            .getResultList();

        try {
            states.prepareNotify(notifications);
        } catch (final TransactionalException oe) {
            if (OptimisticLockException.class.isInstance(oe.getCause())) { // another node did it for us, let him handle that then
                return;
            }
        }

        Stream.of(states.doNotify(activeServices, notifications))
            .forEach(handler -> {
                tracker.put(handler, true);
                handler.whenComplete((r, e) -> tracker.remove(handler));
            });
    }

    @PreDestroy
    private void clean() {
        tracker.forEach((k, v) -> k.cancel(true));
        tracker.clear();
    }

    public Collection<CompletableFuture<?>> getTrackedTasks() {
        return tracker.keySet();
    }

    @Transactional
    @ApplicationScoped
    public static class NotifierStates {
        @Inject
        private EntityManager entityManager;

        public void prepareNotify(final List<Notification> notifications) {
            notifications.stream().forEach(notification -> moveToState(notification, Notification.State.PUBLISHING));
        }

        public CompletableFuture<?>[] doNotify(final SocialService[] activeServices, final List<Notification> notifications) {
            return notifications.stream().map(notification -> {
                moveToState(notification, Notification.State.PUBLISHED);
                return CompletableFuture.allOf(
                    Stream.of(activeServices)
                        .map(a -> a.publish(notification.getText()))
                        .toArray(CompletableFuture[]::new));
            }).toArray(CompletableFuture[]::new); // don't re-aggregate here to avoid to have a master future in case of multiple rows
        }

        private void moveToState(final Notification notification, final Notification.State state) {
            // find it again cause we use 2 tx to ensure we can do the update only once
            entityManager.find(Notification.class, notification.getId()).setState(state);
        }
    }
}
