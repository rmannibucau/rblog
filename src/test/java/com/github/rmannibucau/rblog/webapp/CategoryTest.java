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
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.github.rmannibucau.rblog.jpa.PostType.POST;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.rules.RuleChain.outerRule;

public class CategoryTest {
    private static final String TEST_CATEGORY = "Test Category";

    @Rule
    public final TestRule rule = outerRule(new RBlog.Rule(this)).around(new WebRule(this));

    @Application
    private RBlog blog;

    @FindBy(id = "categoryName")
    private WebElement categoryName;

    @FindBy(id = "categorySlug")
    private WebElement categorySlug;

    @FindBy(id = "categorySubmit")
    private WebElement categorySubmit;

    @FindBy(className = "deleteAction")
    private WebElement delete;

    @FindBy(className = "category")
    private WebElement category;

    @FindBy(className = "post")
    private WebElement post;

    @PersistenceContext
    private EntityManager em;

    @Test
    public void crud() {
        blog.executeInAdminContext(() -> {
            blog.goTo("/admin/category/new");
            blog.waitUntil(() -> blog.isDisplayed(categoryName) && blog.isDisplayed(categorySubmit));

            // create with auto slug_
            blog.setInputText(categoryName, TEST_CATEGORY);
            categorySubmit.click();
            blog.waitUntil(() -> countCategories() == 1);
            blog.waitUntil(() -> !categorySlug.getAttribute("value").isEmpty()); // model is updated
            assertEquals("test-category", categorySlug.getAttribute("value"));

            // update slug
            blog.setInputText(categorySlug, "another-test-category-slug");
            categorySubmit.click();
            blog.waitUntil(() ->
                em.createQuery("select count(c) from Category c where c.name = '" + TEST_CATEGORY + "' and c.slug = 'another-test-category-slug'", Number.class)
                    .getSingleResult().intValue() == 1);
            blog.waitUntil(() -> "another-test-category-slug".equals(categorySlug.getAttribute("value")));

            // go on category list to check it is created
            blog.goTo("/admin/categories");
            blog.waitUntil(() -> blog.isDisplayed(category) && !category.findElements(By.tagName("td")).isEmpty());
            assertTrue(category.getText().replaceAll("[\t ]*", "").endsWith("TestCategoryanother-test-category-slugEditDelete"));

            // delete it
            delete.click();
            blog.waitUntil(() -> countCategories() == 0);
        });
    }

    @Test
    public void categoryPage() {
        final long[] ids = blog.inTx(() -> {
            final Category category = new Category();
            category.setName("Post Category");
            category.setSlug("post-category");
            em.persist(category);

            final Post post = new Post();
            post.setTitle("The Title");
            post.setContent("<p>The Content</p>");
            post.setSummary("Quick notes");
            post.setSlug("the-title");
            post.setType(POST);
            post.setCategories(new ArrayList<>(singletonList(category)));
            category.setPosts(new ArrayList<>(singletonList(post)));
            post.setPublishDate(new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)));
            em.persist(post);

            em.flush();
            return new long[] { post.getId(), category.getId() };
        });
        try {
            blog.goTo("/category/post-category");
            blog.waitUntil(() -> blog.isDisplayed(post));
            assertEquals("http://localhost:" + blog.getHttpPort() + /*"/rblog/#" + */ "/post/the-title", post.findElement(By.tagName("a")).getAttribute("href"));
        } finally {
            blog.inTx(() -> {
                em.remove(em.find(Post.class, ids[0]));
                em.remove(em.find(Category.class, ids[1]));
            });
        }
    }

    private int countCategories() {
        return em.createQuery("select count(c) from Category c where c.name = '" + TEST_CATEGORY + "'", Number.class)
            .getSingleResult().intValue();
    }
}
