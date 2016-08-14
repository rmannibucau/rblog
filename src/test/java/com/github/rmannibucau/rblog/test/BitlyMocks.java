package com.github.rmannibucau.rblog.test;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import static org.junit.Assert.assertEquals;

@Path("bitly-mock")
@ApplicationScoped
public class BitlyMocks {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String get(@QueryParam("access_token") final String token, @QueryParam("longUrl") final String url) {
        assertEquals("testbitly", token);
        assertEquals("http://test#/foo", url);
        return "{\"data\":{\"url\":\"http://short/bitly\"},\"status_code\":200}";
    }
}
