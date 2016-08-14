package com.github.rmannibucau.rblog.jaxrs;

import com.github.rmannibucau.rblog.jaxrs.async.Async;
import com.github.rmannibucau.rblog.security.cdi.Logged;
import com.github.rmannibucau.rblog.service.BitlyService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Logged
@Path("bitly")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class BitlyResource {
    @Inject
    private BitlyService bitly;

    @GET
    @Path("state")
    @Async
    public void isActive(final @Suspended AsyncResponse response) {
        response.resume(new State(bitly.isActive()));
    }

    @POST
    @Path("shorten")
    @Async
    public void shorten(final Url in, final @Suspended AsyncResponse response) {
        if (!bitly.isActive()) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"bitly not activated\"}").build());
        }
        response.resume(new Url(bitly.bitlyize(in.getValue())));
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class State {
        private boolean active;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Url {
        private String value;
    }
}
