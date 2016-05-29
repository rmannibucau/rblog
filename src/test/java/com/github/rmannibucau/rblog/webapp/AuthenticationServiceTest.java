package com.github.rmannibucau.rblog.webapp;

import com.github.rmannibucau.rblog.test.RBlog;
import com.github.rmannibucau.rblog.test.WebRule;
import org.apache.openejb.testing.Application;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.openqa.selenium.WebDriver;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.rules.RuleChain.outerRule;

public class AuthenticationServiceTest {
    @Rule
    public final TestRule rule = outerRule(new RBlog.Rule(this)).around(new WebRule(this));

    @Application
    private RBlog blog;

    @WebRule.Web
    private WebDriver driver;

    @Before
    public void ensureAdminUser() {
        try {
            blog.createUser("admin", "admin");
        } catch (final Exception e) {
            // ok already there
        }
    }

    @After
    public void reset() {
        blog.clean();
    }

    @Test
    public void loginLogout() throws IOException, InterruptedException {
        blog.goTo(null);

        final RBlog.Home home = blog.getPage(RBlog.Home.class);
        blog.waitUntil(() -> blog.isDisplayed(home.loginLink));

        // login button is there but not logout one
        assertTrue(blog.isDisplayed(home.loginLink));
        assertFalse(blog.isDisplayed(home.logoutLink));

        // go on login page now
        home.loginLink.click();

        // check we are on login page
        final RBlog.LoginForm loginForm = blog.getPage(RBlog.LoginForm.class);
        assertTrue(loginForm.login.isDisplayed());
        assertTrue(loginForm.password.isDisplayed());
        assertTrue(loginForm.submit.isDisplayed());
        assertTrue(loginForm.errors.getText().isEmpty());

        // failing login
        loginForm.login(blog, "invalid", "invalid");
        blog.waitUntil(() -> !loginForm.errors.getText().isEmpty());
        assertEquals("Error during login (HTTP 400): bad credentials", loginForm.errors.getText());

        // actual login
        loginForm.login(blog, "admin", "admin");
        blog.waitUntil(() -> blog.isDisplayed(home.logoutLink));
        assertFalse(blog.isDisplayed(home.loginLink));

        // logout
        home.logoutLink.click();
        blog.waitUntil(() -> blog.isDisplayed(home.loginLink));
        assertFalse(blog.isDisplayed(home.logoutLink));
    }
}
