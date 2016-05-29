package com.github.rmannibucau.rblog.service;

import com.github.rmannibucau.rblog.test.RBlog;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class SlugServiceTest {
    @Rule
    public final TestRule container = new RBlog.Rule(this);

    @Parameterized.Parameters(name = "{0}=>{1}")
    public static String[][] parameters() {
        return new String[][]{
            {"Some Nice title", "some-nice-title"},
            {"And with a , comma?", "and-with-a-comma"},
            {"And with accént", "and-with-accent"}
        };
    }

    @Inject
    private SlugService slugService;

    @Parameterized.Parameter(0)
    public String original;

    @Parameterized.Parameter(1)
    public String expected;

    @Test
    public void isEquals() {
        assertEquals(expected, slugService.slugFrom(original));
    }
}
