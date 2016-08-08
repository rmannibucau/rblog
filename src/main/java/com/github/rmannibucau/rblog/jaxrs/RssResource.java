package com.github.rmannibucau.rblog.jaxrs;

import com.github.rmannibucau.rblog.configuration.Configuration;
import com.github.rmannibucau.rblog.jaxrs.async.Async;
import com.github.rmannibucau.rblog.jaxrs.dump.VisitorSupport;
import lombok.Data;

import javax.annotation.PostConstruct;
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
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.MINUTES;
import static javax.ejb.TransactionManagementType.BEAN;
import static javax.xml.bind.annotation.XmlAccessType.FIELD;

@Path("rss")
@Singleton // for @Schedule
@Startup
@TransactionManagement(BEAN)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class RssResource {
    private volatile Rss rss;

    @Inject
    @Configuration("rblog.rss.title")
    private String title;

    @Resource
    private SessionContext sc;

    @Inject
    private VisitorSupport visitor;
    private Future<?> init;

    @PostConstruct
    private void init() {
        init = sc.getBusinessObject(RssResource.class).doGenerate();
    }

    @Asynchronous
    public Future<?> doGenerate() { // some fields are mandatory to build a valid RSS feed, that's why some are hardcoded there
        final AtomLink atomLink = newChannelAtomLink();
        final Channel channel = newChannel(atomLink);

        visitor.visit(p -> {
            final String url = visitor.buildLink(p);

            final Guid guid = new Guid();
            guid.setValue(url + "?rssGuid=" + Long.toString(p.getId()));

            final AtomLink link = new AtomLink();
            link.setRel("self");
            link.setType(atomLink.getType());
            link.setHref(atomLink.getHref() + "?post=" + p.getId());

            final Item item = new Item();
            item.setTitle(p.getTitle());
            item.setLink(url);
            item.setDescription(p.getSummary());
            item.setPubDate(RFC_1123_DATE_TIME.format(ZonedDateTime.ofInstant(p.getUpdated().toInstant(), ZoneId.systemDefault())));
            item.setGuid(guid);
            item.setAtomLink(link);
            channel.getItem().add(item);
        });

        final Rss rss = new Rss();
        rss.setChannel(channel);
        this.rss = rss;
        return new AsyncResult<>(true);
    }

    private Channel newChannel(final AtomLink atomLink) {
        final Channel channel = new Channel();
        channel.setTitle(ofNullable(title).orElse("RBlog"));
        channel.setDescription("RBlog RSS feed");
        channel.setLink(visitor.getBase());
        channel.setGenerator("RBlog RSS Generator");
        channel.setUpdateFrequency("1");
        channel.setUpdatePeriod("daily");
        channel.setAtomLink(atomLink);
        channel.setItem(new ArrayList<>());
        return channel;
    }

    private AtomLink newChannelAtomLink() {
        final AtomLink atomLink = new AtomLink();
        atomLink.setRel("self");
        atomLink.setType("application/rss+xml");
        atomLink.setHref(visitor.getBase() + "/api/rss");
        return atomLink;
    }

    @Schedule(persistent = false) // daily
    public void regenerate() {
        doGenerate();
    }

    @GET
    @Async
    @Produces(MediaType.APPLICATION_XML)
    public void get(@QueryParam("post") final String postId, @Suspended final AsyncResponse response) {
        try { // try to ensure we tried to get few data before timing out
            init.get(1, MINUTES);
        } catch (final InterruptedException e) {
            Thread.interrupted();
        } catch (final ExecutionException | TimeoutException e) {
            // let's return what we have, likely null but don't block more!
        }
        response.resume(ofNullable(postId)
                .map(id -> {
                    final Rss copy = new Rss();
                    copy.setVersion(rss.getVersion());
                    copy.setChannel(newChannel(newChannelAtomLink()));
                    for (final Item i : rss.getChannel().getItem()) {
                        if (i.getGuid().getValue().endsWith("?rssGuid=" + postId)) {
                            copy.getChannel().getItem().add(i);
                            break;
                        }
                    }
                    return copy;
                }).orElse(rss));
    }

    @Data
    @XmlAccessorType(FIELD)
    @XmlRootElement(name = "rss")
    public static class Rss {
        @XmlAttribute(name = "version")
        private String version = "2.0";

        @XmlElement
        private Channel channel;
    }

    @Data
    @XmlType
    @XmlAccessorType(FIELD)
    public static class Channel {
        @XmlElement
        private String title;

        @XmlElement
        private String link;

        @XmlElement(name = "link", namespace = "http://www.w3.org/2005/Atom")
        private AtomLink atomLink;

        @XmlElement
        private String description;

        @XmlElement
        private String generator;

        @XmlElement
        private List<Item> item;

        @XmlElement(namespace = "http://purl.org/rss/1.0/modules/syndication/")
        private String updatePeriod;

        @XmlElement(namespace = "http://purl.org/rss/1.0/modules/syndication/")
        private String updateFrequency;
    }

    @Data
    @XmlType(namespace = "http://www.w3.org/2005/Atom")
    @XmlAccessorType(FIELD)
    public static class AtomLink {
        @XmlAttribute
        private String rel;

        @XmlAttribute
        private String type;

        @XmlAttribute
        private String href;

        @XmlAttribute
        private String title;
    }

    @Data
    @XmlType
    @XmlAccessorType(FIELD)
    public static class Item {
        @XmlElement(name = "link", namespace = "http://www.w3.org/2005/Atom")
        private AtomLink atomLink;

        @XmlElement
        private String title;

        @XmlElement
        private String link;

        @XmlElement
        private String pubDate;

        @XmlElement
        private String description;

        @XmlElement
        private Guid guid;
    }

    @Data
    @XmlType
    @XmlAccessorType(FIELD)
    public static class Guid {
        @XmlAttribute
        private boolean isPermaLink;

        @XmlValue
        private String value;
    }
}
