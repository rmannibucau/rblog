package com.github.rmannibucau.rblog.test;

import com.github.rmannibucau.rblog.service.BitlyService;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import static org.junit.Assert.assertEquals;

@Path("bitly-mock")
@ApplicationScoped
public class BitlyMocks {
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public String post(@HeaderParam(HttpHeaders.AUTHORIZATION) final String token,
                       final BitlyService.BitlyData data) {
        assertEquals("Bearer testbitly", token);
        assertEquals("http://test#/foo", data.getLong_url());
        return "{\"link\":\"http://short/bitly\"}";
    }
}
