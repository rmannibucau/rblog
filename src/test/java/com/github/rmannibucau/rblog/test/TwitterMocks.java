package com.github.rmannibucau.rblog.test;

import com.github.rmannibucau.rblog.social.TwitterService;
import lombok.Getter;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Specializes;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

@Path("1.1")
@ApplicationScoped
public class TwitterMocks {
    @Getter
    private final Map<String, String> values = new HashMap<>();

    @POST
    @Path("statuses/update.json")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public String post(final String body, @HeaderParam("Authorization") final String signature) {
        values.put("body", body);
        values.put("authorization", signature);
        return "{}"; // status only is used
    }

    public void reset() {
        values.clear();
    }

    @Specializes
    @ApplicationScoped
    public static class TimestampTestProvider extends TwitterService.TimestampProvider {
        @Override
        public long now() {
            return 123456789;
        }
    }

    @Specializes
    @ApplicationScoped
    public static class NonceTestProvider extends TwitterService.NonceProvider {
        @Override
        public String next() {
            return "zfcfrjflefnjl";
        }
    }
}
