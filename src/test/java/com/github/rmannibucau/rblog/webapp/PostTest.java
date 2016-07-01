package com.github.rmannibucau.rblog.webapp;

import com.github.rmannibucau.rblog.jpa.Category;
import com.github.rmannibucau.rblog.jpa.Post;
import com.github.rmannibucau.rblog.test.RBlog;
import com.github.rmannibucau.rblog.test.WebRule;
import org.apache.openejb.testing.Application;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchFrameException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.github.rmannibucau.rblog.jpa.PostType.POST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.rules.RuleChain.outerRule;

public class PostTest {
    @Rule
    public final TestRule rule = outerRule(new RBlog.Rule(this)).around(new WebRule(this));

    @Application
    private RBlog blog;

    @WebRule.Web
    private WebDriver browser;

    @FindBy(id = "postTitle")
    private WebElement postTitle;

    @FindBy(id = "postSlug")
    private WebElement postSlug;

    @FindBy(id = "postSummary")
    private WebElement postSummary;

    @FindBy(id = "postPublishDate")
    private WebElement postPublishDate;

    @FindBy(id = "postCategories")
    private WebElement postCategories;

    @FindBy(id = "postSubmit")
    private WebElement postSubmit;

    @FindBy(className = "deleteAction")
    private WebElement delete;

    @FindBy(className = "post")
    private WebElement post;

    @FindBy(id = "postContent")
    private WebElement postContent;

    @PersistenceContext
    private EntityManager em;

    @Test
    public void crud() {
        final long catId = blog.inTx(() -> {
            final Category category = new Category();
            category.setName("Post Category");
            em.persist(category);
            em.flush();
            return category.getId();
        });
        try {
            blog.executeInAdminContext(() -> {
                blog.goTo("/admin/post/new");
                blog.waitUntil(() -> blog.isDisplayed(postTitle) && blog.isDisplayed(postCategories) && isContentShown());

                // create with auto slug, auto category and user
                blog.setInputText(postTitle, "Test Post");
                blog.setInputText(postSummary, "Some summarry");
                // TODO: postPublishDate
                new Select(postCategories).selectByVisibleText("Post Category");

                // postEditor is in an iframe
                blog.setInputText(findPostContent(), "Some content");
                browser.switchTo().parentFrame(); // back to main one

                postSubmit.click();
                blog.waitUntil(() -> count() == 1);

                editPost();
                blog.waitUntil(() -> !postSlug.getAttribute("value").isEmpty()); // model is updated
                assertEquals("test-post", postSlug.getAttribute("value"));

                // update slug
                blog.setInputText(postSlug, "alternative-post");
                postSubmit.click();
                blog.waitUntil(() ->
                    em.createQuery("select count(c) from Post c where c.title = 'Test Post' and c.slug = 'alternative-post'", Number.class)
                        .getSingleResult().intValue() == 1);

                editPost();
                blog.waitUntil(() -> "alternative-post".equals(postSlug.getAttribute("value")));

                // go on category list to check it is created
                blog.goTo("/admin/posts");
                blog.waitUntil(() -> blog.isDisplayed(post) && !post.findElements(By.tagName("td")).isEmpty());

                final String line = post.getText();
                assertTrue(line.contains("Test Post"));
                assertTrue(line.contains("alternative-post"));
                assertTrue(line.contains("admin"));

                // delete it
                delete.click();
                blog.waitUntil(() -> count() == 0);
            });
        } finally {
            blog.inTx(() -> em.remove(em.find(Category.class, catId)));
        }
    }

    private void editPost() {
        blog.goTo("/admin/post/" + em.createQuery("select c from Post c where c.title = 'Test Post'", Post.class).getSingleResult().getId());
    }

    @Test
    public void postPage() {
        final long id = blog.inTx(() -> {
            final Post post = new Post();
            post.setTitle("The Title");
            post.setContent("<p>The Content</p>");
            post.setSummary("Quick notes");
            post.setSlug("the-title");
            post.setType(POST);
            post.setPublishDate(new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)));
            em.persist(post);
            em.flush();
            return post.getId();
        });
        try {
            blog.goTo("/post/the-title");
            blog.waitUntil(() -> blog.isDisplayed(postTitle) && !postTitle.getText().isEmpty() && blog.isDisplayed(postContent) && !postContent.getText().isEmpty());
            assertEquals("The Title", postTitle.getText());
            assertEquals("The Content", postContent.getText());
        } finally {
            blog.inTx(() -> em.remove(em.find(Post.class, id)));
        }
    }

    private WebElement findPostContent() {
        return browser.switchTo().frame(0).findElement(By.xpath("/html/body"));
    }

    private boolean isContentShown() {
        try {
            final WebDriver frame = browser.switchTo().frame(0);
            try {
                blog.isDisplayed(frame.findElement(By.xpath("/html/body")));
            } finally {
                frame.switchTo().parentFrame();
            }
            return true;
        } catch (final NoSuchFrameException nsfe) {
            return false;
        }
    }

    private int count() {
        return em.createQuery("select count(c) from Post c where c.title = 'Test Post'", Number.class)
            .getSingleResult().intValue();
    }
}
