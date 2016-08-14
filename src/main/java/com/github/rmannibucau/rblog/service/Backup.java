package com.github.rmannibucau.rblog.service;

import com.github.rmannibucau.rblog.configuration.Configuration;
import com.github.rmannibucau.rblog.event.DoBackup;
import com.github.rmannibucau.rblog.jpa.Category;
import com.github.rmannibucau.rblog.jpa.Post;
import com.github.rmannibucau.rblog.jpa.User;
import lombok.extern.java.Log;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.transaction.Transactional;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.function.Consumer;
import java.util.stream.LongStream;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;

// using a polling strategy allows to not be in the same thread and avoid concurrency issues
@Log
@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionManagement(TransactionManagementType.BEAN)
public class Backup {
    @Resource
    private SessionContext ctx;

    @Inject
    private Internal internal;

    @Resource
    private TimerService timerService;

    @Inject
    @Configuration("${rblog.backup.polling:PT1M}") // java.time.Duration syntax, 1 minute
    private String validDuration;

    private volatile boolean doBackup;
    private volatile Timer timer;

    @PostConstruct
    private void init() {
        final long period = Duration.parse(validDuration).toMillis();
        timer = timerService.createIntervalTimer(period, period, new TimerConfig("backup", false));
    }

    @PreDestroy
    private void destroy() {
        timer.cancel();
    }

    // @ForTests
    public void reset() {
        destroy();
        doBackup = false;
        init();
    }

    @Timeout
    public void backup(final Timer timer) {
        if (doBackup && internal.isActive()) {
            doBackup = false;
            internal.doBackup();
        }
    }

    public void doBackup(@Observes final DoBackup event) {
        doBackup = true;
    }

    @ApplicationScoped
    public static class Internal {
        @PersistenceContext
        private EntityManager em;

        @Inject
        @Configuration("${rblog.pagination.max:100}")
        private Integer paginationSize;

        @Inject
        @Configuration("${rblog.backup.work:${openejb.base}/work/rblog}")
        private String workDir;

        @Inject
        @Configuration("rblog.backup.mail.sessionJndi")
        private String sessionJndi;

        @Inject
        @Configuration("${rblog.backup.mail.from:rblog@rblog.com}")
        private String from;

        @Inject
        @Configuration("${rblog.backup.mail.subject:RBlog backup}")
        private String subject;

        @Inject
        @Configuration("rblog.backup.mail.to")
        private String to;

        private JsonGeneratorFactory generatorFactory;
        private Session session;

        @PostConstruct
        private void init() {
            if (isActive()) {
                try {
                    session = Session.class.cast(new InitialContext().lookup(sessionJndi));
                } catch (final NamingException e) {
                    throw new IllegalArgumentException(e);
                }
                generatorFactory = Json.createGeneratorFactory(emptyMap());
            }
        }

        @Transactional
        public void doBackup() {
            final File out = new File(workDir, "backup_" + System.currentTimeMillis() + ".json");
            out.getParentFile().mkdirs();

            // using streaming (pagination for JPA) to not load the whole DB in memory
            try (final JsonGenerator generator = generatorFactory.createGenerator(new ZipOutputStream(new FileOutputStream(out)) {
                {
                    super.setLevel(Deflater.BEST_COMPRESSION);

                    try {
                        super.putNextEntry(new ZipEntry("backup.json"));
                    } catch (final IOException e) {
                        throw new IllegalStateException(e);
                    }
                }

                @Override
                public void close() throws IOException {
                    super.closeEntry();
                    super.close();
                }
            })) {
                generator.writeStartObject();
                generator.write("date", LocalDateTime.now().toString());

                generator.writeStartArray("categories");
                browse(Category.class, cat -> {
                    generator.writeStartObject();
                    generator.write("id", cat.getId());
                    ofNullable(cat.getName()).ifPresent(c -> generator.write("name", c));
                    ofNullable(cat.getSlug()).ifPresent(c -> generator.write("slug", c));
                    ofNullable(cat.getParent()).ifPresent(p -> generator.write("parentId", p.getId()));
                    ofNullable(cat.getColor()).ifPresent(c -> generator.write("color", c));
                    generator.writeEnd();
                });
                generator.writeEnd();

                generator.writeStartArray("posts");
                browse(Post.class, post -> {
                    generator.writeStartObject();
                    generator.write("id", post.getId());
                    ofNullable(post.getTitle()).ifPresent(s -> generator.write("title", s));
                    ofNullable(post.getAuthor()).ifPresent(a -> generator.write("author", a.getUsername()));
                    ofNullable(post.getSlug()).ifPresent(c -> generator.write("slug", c));
                    ofNullable(post.getType()).ifPresent(t -> generator.write("type", t.name()));
                    ofNullable(post.getContent()).ifPresent(c -> generator.write("content", c));
                    ofNullable(post.getSummary()).ifPresent(s -> generator.write("summary", s));
                    ofNullable(post.getPublishDate()).ifPresent(d -> generator.write("published", newDateFormat().format(post.getPublishDate())));
                    ofNullable(post.getCreated()).ifPresent(d -> generator.write("created", newDateFormat().format(post.getPublishDate())));
                    ofNullable(post.getUpdated()).ifPresent(d -> generator.write("updated", newDateFormat().format(post.getPublishDate())));
                    ofNullable(post.getCategories()).ifPresent(categories -> {
                        generator.writeStartObject("categories");
                        categories.forEach(cat -> {
                            generator.write("id", cat.getId());
                            ofNullable(cat.getName()).ifPresent(c -> generator.write("name", c));
                        });
                        generator.writeEnd();
                    });
                    generator.writeEnd();
                });
                generator.writeEnd();

                generator.writeStartArray("users");
                browse(User.class, cat -> {
                    generator.writeStartObject();
                    generator.write("id", cat.getId());
                    ofNullable(cat.getUsername()).ifPresent(u -> generator.write("username", u));
                    ofNullable(cat.getMail()).ifPresent(u -> generator.write("mail", u));
                    ofNullable(cat.getDisplayName()).ifPresent(u -> generator.write("displayName", u));
                    // password could be regenerated if needed
                    // no need to save token, it is fine to have to log in again
                    generator.writeEnd();
                });
                generator.writeEnd();

                generator.writeEnd();
            } catch (final FileNotFoundException e) {
                throw new IllegalStateException(e);
            }

            log.info("Saved " + out.getAbsolutePath());

            try {
                sendMail(out);
                log.info("Backup mail sent.");
            } catch (final MessagingException e) {
                throw new IllegalStateException(e);
            } finally {
                if (!out.delete()) {
                    out.deleteOnExit();
                }
            }
        }

        public boolean isActive() {
            return sessionJndi != null && to != null;
        }

        private void sendMail(final File out) throws MessagingException {
            final Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText("A new backup has been done.");

            final MimeBodyPart messageBodyPart = new MimeBodyPart();
            final Multipart multipart = new MimeMultipart();

            final DataSource source = new FileDataSource(out);
            messageBodyPart.setDataHandler(new DataHandler(source));
            messageBodyPart.setFileName("rblog_" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".zip");
            multipart.addBodyPart(messageBodyPart);
            message.setContent(multipart);

            Transport.send(message);
        }

        private SimpleDateFormat newDateFormat() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }

        private long toPages(final long total) {
            long pages = total / paginationSize;
            if (total - (pages * paginationSize) > 0) {
                return pages + 1;
            }
            return pages;
        }

        private <T> void browse(final Class<T> entity, final Consumer<T> consumer) {
            LongStream.range(0, toPages(count(entity))).forEach(p -> {
                final CriteriaBuilder builder = em.getCriteriaBuilder();
                final CriteriaQuery<T> query = builder.createQuery(entity);
                em.createQuery(query.select(query.from(entity))).getResultList().forEach(consumer);
            });
        }

        private long count(final Class<?> entity) {
            final CriteriaBuilder builder = em.getCriteriaBuilder();
            final CriteriaQuery<Number> query = builder.createQuery(Number.class);
            return em.createQuery(query.select(builder.count(query.from(entity)))).getSingleResult().longValue();
        }
    }
}
