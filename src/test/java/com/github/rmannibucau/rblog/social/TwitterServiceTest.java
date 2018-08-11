package com.github.rmannibucau.rblog.social;

import com.github.rmannibucau.rblog.service.URLService;
import com.github.rmannibucau.rblog.test.RBlog;
import com.github.rmannibucau.rblog.test.TwitterMocks;
import org.apache.openejb.testing.Application;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(RBlog.Runner.class)
public class TwitterServiceTest {
    @Application
    private RBlog blog;

    @Inject
    private TwitterService twitterService;

    @Inject
    private URLService urlService;

    @Inject
    private TwitterMocks mock;

    @Before
    @After
    public void before() {
        mock.reset();
    }

    @Test
    public void length() {
        final Map<String, Integer> messages = new LinkedHashMap<>();
        messages.put("Simple tweet", 12);
        messages.put("Simple tweet with trailing http://link", 50);
        messages.put("Simple tweet with trailing https://link", 50);
        messages.put("Simple tweet with trailing http://link?q=true", 50);
        messages.put("Simple tweet with trailing https://link?q=true", 50);
        messages.put("Simple tweet with trailing http://link-test.com/test?gdbezudh-iphen", 50);
        messages.put("Simple tweet with trailing https://link-test.com/test?gdbezudh-iphen", 50);
        messages.put("Simple tweet with a http://link in the middle", 57);
        messages.put("Simple tweet with trailing http://link?q=true in the middle", 64);
        messages.put("Simple tweet with trailing http://link-test.com/test?gdbezudh-iphen in the middle", 64);
        messages.put("Simple tweet with a\nhttp://link in the middle", 57);
        messages.put("Simple tweet with trailing https://link?q=true\nin the middle", 64);
        messages.put("Simple tweet with\ntrailing http://link-test.com/test?gdbezudh-iphen in\nthe middle", 64);
        messages.put("http://link for a simple tweet", 42);
        messages.put("https://link for a simple tweet", 42);
        messages.put("http://link\nfor a simple tweet", 42);
        messages.put("https://link\nfor a simple tweet", 42);
        messages.entrySet()
                .forEach(tweet -> assertEquals(tweet.getKey(), tweet.getValue().intValue(), twitterService.messageLength(tweet.getKey())));
    }

    @Test
    public void validate() {
        try {
            twitterService.validate(
                    "Super new post on the blog\n\n" +
                    ">>>> http://bit.ly/GDBEHIKBD3748 <<<<\n\n" +
                    "About a super topic\n\n#hash #tag #super #yeah\n\n" +
                    "Come and read more about it ASAP!\n");
        } catch (final IllegalArgumentException iae) {
            assertEquals("Message too long: 143 instead of 140", iae.getMessage());
        }
    }

    @Test
    public void tweet() throws IOException {
        assertTrue(twitterService.isActive());
        try {
            twitterService.publish("IGNORE TEST TWEET: #withHandle // cc #javaee ?&\"è_ç('éà') => http://localhost.test#/post/super-post").get();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            fail();
        } catch (final ExecutionException e) {
            final StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            fail(e.getMessage() + "\n" + writer.toString());
        }
        assertEquals("status=IGNORE%20TEST%20TWEET%3A%20%23withHandle%20%2F%2F%20cc%20%23javaee%20%3F%26%22%C3%A8_%C3%A7%28%27%C3%A9%C3%A0%27%29%20%3D%3E%20http%3A%2F%2Flocalhost.test%23%2Fpost%2Fsuper-post", mock.getValues().get("body"));
        assertEquals("OAuth oauth_consumer_key=\"C1245678OUhejdve\", oauth_nonce=\"zfcfrjflefnjl\", oauth_signature=\"" + signature() + "\", oauth_signature_method=\"HMAC-SHA1\", oauth_timestamp=\"123456789\", oauth_token=\"123486489-frce5s4HIKVFRF\", oauth_version=\"1.0\"", mock.getValues().get("authorization"));
    }

    private String signature() { // port is dynamic so we need to recompute a part
        try {
            final SecretKeySpec key = new SecretKeySpec("6s4dfzfrr7JGKRFD&Jbgjczdkl454fecfregfergf".getBytes(StandardCharsets.UTF_8), "HmacSHA1");
            final Mac mac = Mac.getInstance(key.getAlgorithm());
            mac.init(key);
            return urlService.percentEncode(Base64.getEncoder().encodeToString(mac.doFinal(
                ("POST&http%3A%2F%2Flocalhost%3A" + blog.getHttpPort() + "%2Fapi%2F1.1%2Fstatuses%2Fupdate.json&" +
                    "oauth_consumer_key%3DC1245678OUhejdve%26oauth_nonce%3Dzfcfrjflefnjl%26" +
                    "oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D123456789%26" +
                    "oauth_token%3D123486489-frce5s4HIKVFRF%26" +
                    "oauth_version%3D1.0%26" +
                    "status%3DIGNORE%2520TEST%2520TWEET%253A%2520%2523withHandle%2520%252F%252F%2520cc%2520%2523javaee%2520%253F%2526%2522%25C3%25A8_%25C3%25A7%2528%2527%25C3%25A9%25C3%25A0%2527%2529%2520%253D%253E%2520http%253A%252F%252Flocalhost.test%2523%252Fpost%252Fsuper-post").getBytes(StandardCharsets.UTF_8))));
        } catch (final InvalidKeyException | NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
