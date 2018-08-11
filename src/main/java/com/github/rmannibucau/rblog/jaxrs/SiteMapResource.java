package com.github.rmannibucau.rblog.jaxrs;

import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.joining;
import static javax.ejb.TransactionManagementType.BEAN;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Schedule;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionManagement;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.github.rmannibucau.rblog.configuration.Configuration;
import com.github.rmannibucau.rblog.jaxrs.async.Async;
import com.github.rmannibucau.rblog.jaxrs.dump.VisitorSupport;
import com.github.rmannibucau.rblog.jpa.Post;
import com.github.rmannibucau.rblog.security.cdi.Logged;

import lombok.extern.java.Log;

@Log
@Path("sitemap")
@Singleton // for @Schedule
@Startup
@TransactionManagement(BEAN)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class SiteMapResource {
    private volatile Urlset sitemap;

    @Inject
    @Configuration("${rblog.sitemap.pingUrls:http://www.google.com/webmasters/tools/ping?sitemap=%s,http://www.bing.com/ping?sitemap=%s}")
    private String pingUrls;

    @Inject
    @Configuration("${rblog.sitemap.skip:false}")
    private Boolean skip;

    @Resource
    private SessionContext sc;

    @Inject
    private VisitorSupport visitor;

    private Future<?> init;
    private Optional<WebTarget[]> pingTargets;
    private final Collection<Runnable> destroys = new ArrayList<>();
    private final ZoneId gmt = ZoneId.of("GMT");;

    @PostConstruct
    private void init() {
        init = sc.getBusinessObject(SiteMapResource.class).doGenerate();

        // simple way to not create any client if we have no ping url
        // and to not create one per url
        final Supplier<Client> lazyClient = new Supplier<Client>() {
            private Client c;

            @Override
            public Client get() {
                if (c == null) {
                    c = ClientBuilder.newBuilder().build();
                    destroys.add(() -> c.close());
                }
                return c;
            }
        };

        // prepare ping targets
        pingTargets = ofNullable(visitor.getBase())
                .filter(b -> !skip)
                .map(b -> {
                    try {
                        return URLEncoder.encode(visitor.getBase() + "/api/sitemap", "UTF-8");
                    } catch (final UnsupportedEncodingException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .map(u -> Stream.of(pingUrls.split(" *, *"))
                        .map(String::trim)
                        .filter(pu -> !pu.isEmpty())
                        .map(s -> String.format(s, u))
                        .map(url -> lazyClient.get().target(url))
                        .toArray(WebTarget[]::new))
                .filter(targets -> targets.length > 0);
    }

    @PreDestroy
    private void destroy() {
        destroys.forEach(Runnable::run);
    }

    private void addToMap(final Urlset newMap, final Post post) {
        final TUrl url = new TUrl();
        url.setChangefreq(TChangeFreq.WEEKLY); // let's assume that for now
        url.setLastmod(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.ofInstant(post.getUpdated().toInstant(), gmt)));
        url.setPriority(.5); // TODO: add it in Post table
        url.setLoc(visitor.buildLink(post));

        newMap.getUrl().add(url);
    }

    @Schedule(persistent = false) // daily
    public void regenerate() {
        doGenerate();
    }

    @Asynchronous
    public Future<?> doGenerate() {
        final Urlset newMap = new Urlset();
        visitor.visit(p -> addToMap(newMap, p));
        sitemap = newMap;
        ping();

        return new AsyncResult<>(true);
    }

    private void ping() {
        pingTargets.ifPresent(targets -> {
            synchronized (targets) {
                Stream.of(targets)
                        .map(target -> target.request().get().getStatus())
                        .filter(status -> status != HttpURLConnection.HTTP_OK)
                        .forEach(badStatus -> log.warning("Got HTTP " + badStatus + " pinging one of " + pingUrls));
            }
        });
    }

    @GET
    @Async
    @Produces(MediaType.APPLICATION_XML)
    public void get(@Suspended final AsyncResponse response) {
        tryToEnsureSomeData();
        response.resume(toXml(sitemap));
    }

    @HEAD
    @Async
    @Logged
    public void forceUpdate(@Suspended final AsyncResponse response) {
        doGenerate();
    }

    // avoid jaxb namespacing
    private String toXml(final Urlset sitemap) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<urlset\n" +
                "      xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"\n" +
                "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "      xsi:schemaLocation=\"http://www.sitemaps.org/schemas/sitemap/0.9\n" +
                "            http://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd\">\n" +
                sitemap.getUrl().stream().map(it -> "<url>\n" +
                        "  <loc>" + it.getLoc() + "</loc>\n" +
                        "  <lastmod>" + it.getLastmod() + "</lastmod>\n" +
                        "  <priority>" + it.getPriority() + "</priority>\n" +
                        "  <changefreq>" + it.getChangefreq().value() + "</changefreq>\n" +
                        "</url>\n").collect(joining("\n")) +
                "</urlset>";
    }

    private void tryToEnsureSomeData() {
        try {
            init.get(1, MINUTES);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (final ExecutionException | TimeoutException e) {
            // let's return what we have, likely null but don't block more!
        }
    }

    @XmlType(name = "tChangeFreq", namespace = "http://www.sitemaps.org/schemas/sitemap/0.9")
    @XmlEnum
    public enum TChangeFreq {
        @XmlEnumValue("always")
        ALWAYS("always"),

        @XmlEnumValue("hourly")
        HOURLY("hourly"),

        @XmlEnumValue("daily")
        DAILY("daily"),

        @XmlEnumValue("weekly")
        WEEKLY("weekly"),

        @XmlEnumValue("monthly")
        MONTHLY("monthly"),

        @XmlEnumValue("yearly")
        YEARLY("yearly"),

        @XmlEnumValue("never")
        NEVER("never");

        private final String value;

        TChangeFreq(final String v) {
            value = v;
        }

        public String value() {
            return value;
        }

        public static TChangeFreq fromValue(final String v) {
            return Stream.of(TChangeFreq.values())
                    .filter(c -> c.value.equals(v))
                    .findAny()
                    .orElseThrow(() -> new IllegalArgumentException(v));
        }

    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "tUrl", propOrder = {
            "loc",
            "lastmod",
            "changefreq",
            "priority"
    }, namespace = "http://www.sitemaps.org/schemas/sitemap/0.9")
    public static class TUrl {
        @XmlElement(required = true, namespace = "http://www.sitemaps.org/schemas/sitemap/0.9")
        private String loc;

        @XmlElement(namespace = "http://www.sitemaps.org/schemas/sitemap/0.9")
        private String lastmod;

        @XmlElement(namespace = "http://www.sitemaps.org/schemas/sitemap/0.9")
        private TChangeFreq changefreq;

        @XmlElement(namespace = "http://www.sitemaps.org/schemas/sitemap/0.9")
        private double priority;

        public String getLoc() {
            return loc;
        }

        public void setLoc(final String value) {
            this.loc = value;
        }

        public String getLastmod() {
            return lastmod;
        }

        public void setLastmod(final String value) {
            this.lastmod = value;
        }

        public TChangeFreq getChangefreq() {
            return changefreq;
        }

        public void setChangefreq(final TChangeFreq value) {
            this.changefreq = value;
        }

        public double getPriority() {
            return priority;
        }

        public void setPriority(final double value) {
            this.priority = value;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "urlset", namespace = "http://www.sitemaps.org/schemas/sitemap/0.9")
    public static class Urlset {
        @XmlElement(required = true, namespace = "http://www.sitemaps.org/schemas/sitemap/0.9")
        private List<TUrl> url;

        public List<TUrl> getUrl() {
            if (url == null) {
                url = new ArrayList<>();
            }
            return this.url;
        }

    }

}
