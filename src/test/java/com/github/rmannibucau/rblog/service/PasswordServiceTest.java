package com.github.rmannibucau.rblog.service;

import com.github.rmannibucau.rblog.test.RBlog;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;

@RunWith(RBlog.Runner.class)
public class PasswordServiceTest {
    @Inject
    private PasswordService service;

    @Test
    public void convert() {
        assertEquals("1690316BCF298DF9B9F082E155979C90A5CFD663C6AA9117EBF16BF49EE16B6E", service.toDatabaseFormat("secret"));
    }
}
