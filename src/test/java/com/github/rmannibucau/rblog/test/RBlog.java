package com.github.rmannibucau.rblog.test;

import com.github.rmannibucau.rblog.jaxrs.provider.JSonProvider;
import com.github.rmannibucau.rblog.jpa.Token;
import com.github.rmannibucau.rblog.jpa.User;
import com.github.rmannibucau.rblog.security.web.model.Credentials;
import com.github.rmannibucau.rblog.security.web.model.TokenValue;
import com.github.rmannibucau.rblog.service.PasswordService;
import com.google.common.base.Function;
import lombok.Getter;
import org.apache.catalina.Context;
import org.apache.openejb.testing.Application;
import org.apache.openejb.testing.ContainerProperties;
import org.apache.openejb.testing.RandomPort;
import org.apache.tomee.embedded.Configuration;
import org.apache.tomee.embedded.Container;
import org.apache.tomee.loader.TomcatHelper;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.inject.OWBInjector;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.Vetoed;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.servlet.ServletException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

@ContainerProperties({
    @ContainerProperties.Property(name = "rblogDataSource", value = "new://Resource?type=DataSource"),
    @ContainerProperties.Property(name = "rblogDataSource.JdbcDriver", value = "org.h2.Driver"),
    @ContainerProperties.Property(name = "rblogDataSource.JdbcUrl", value = "jdbc:h2:mem:rblog"),
    // @ContainerProperties.Property(name = "rblog.LogSql", value = "true"),

    // don't worry, these are random
    @ContainerProperties.Property(name = "rblog.twitter.consumerKey", value = "C1245678OUhejdve"),
    @ContainerProperties.Property(name = "rblog.twitter.consumerSecret", value = "6s4dfzfrr7JGKRFD"),
    @ContainerProperties.Property(name = "rblog.twitter.token", value = "123486489-frce5s4HIKVFRF"),
    @ContainerProperties.Property(name = "rblog.twitter.tokenSecret", value = "Jbgjczdkl454fecfregfergf"),
    @ContainerProperties.Property(name = "rblog.twitter.api.update.url", value = "http://localhost:${http.port}/rblog/api/1.1/statuses/update.json"),

    // we need to ensure we use the DB the test expects and the test can clean it so ensure it starting from an empty DB
    @ContainerProperties.Property(name = "rblog.provisioning.defaultUser.active", value = "false"),

    // test == dev
    @ContainerProperties.Property(name = "rblog.environment", value = "dev"),

    // nicer logging
    @ContainerProperties.Property(name = "openejb.jul.forceReload", value = "true")
})
public class RBlog {
    @RandomPort("http")
    private URL base;

    @Inject
    private UserTransaction ut;

    @Inject
    private EntityManager entityManager;

    @Inject
    private PasswordService passwordService;

    @Getter
    private String baseUrl;

    private WebDriver webDriver;
    private WebDriverWait waitDriver;

    @PostConstruct
    private void initBase() throws MalformedURLException, ServletException {
        /* for JAXRS debugging
        final Bus bus = CxfUtil.getBus();
        bus.getInInterceptors().add(new LoggingInInterceptor());
        bus.getInFaultInterceptors().add(new LoggingInInterceptor());
        bus.getOutInterceptors().add(new LoggingOutInterceptor());
        bus.getOutFaultInterceptors().add(new LoggingOutInterceptor());
        */
        baseUrl = base.toExternalForm() + "rblog/";
    }

    @PreDestroy
    private void clear() {
        ofNullable(webDriver).ifPresent(WebDriver::quit);
    }

    public int getHttpPort() {
        return base.getPort();
    }

    public WebDriver browser() {
        return webDriver == null ? (webDriver = WebDriverFactory.createDriver()) : webDriver;
    }

    public boolean isDisplayed(final WebElement elt) {
        try {
            return elt.isDisplayed();
        } catch (final NoSuchElementException nse) {
            return false;
        }
    }

    public <T> T injectWeb(final T instance) {
        PageFactory.initElements(browser(), instance);
        return instance;
    }

    public <T> T getPage(final Class<T> type) {
        return PageFactory.initElements(browser(), type);
    }

    public void goTo(final String page) {
        final WebDriver browser = browser();
        browser.get(baseUrl + "#" + ofNullable(page).orElse(""));
        browser.manage().window().maximize();
    }

    public WebTarget target() {
        return ClientBuilder.newBuilder().register(new JSonProvider<>()).build().target(baseUrl + "api");
    }

    public void withTempUser(final BiConsumer<Long, String> tokenConsummer) {
        final User user = createUser("test", "test");
        try {
            withToken("test", "test", token -> tokenConsummer.accept(user.getId(), token));
        } finally {
            deleteUser(user.getId());
        }
    }

    public void withToken(final String user, final String pwd, final Consumer<String> tokenConsummer) {
        final String token = target().path("security/login").request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(new Credentials() {
                    {
                        setUsername(user);
                        setPassword(pwd);
                    }
                }, MediaType.APPLICATION_JSON_TYPE), TokenValue.class).getToken();
        try {
            tokenConsummer.accept(token);
        } finally {
            inTx(() -> { // cleanup
                entityManager.remove(entityManager.getReference(Token.class, token));
            });
        }
    }

    public User createUser(final String name, final String pwd) {
        return inTx(() -> {
            final User u = new User();
            u.setUsername(name);
            u.setPassword(passwordService.toDatabaseFormat(pwd));
            u.setDisplayName(name);
            entityManager.persist(u);
            return u;
        });
    }

    public void deleteUser(final long id) {
        inTx(() -> entityManager.remove(entityManager.find(User.class, id)));
    }

    public void inTx(final Runnable task) {
        inTx(() -> {
            task.run();
            return null;
        });
    }

    public <T> T inTx(final Supplier<T> task) {
        try {
            ut.begin();
            final T t = task.get();
            ut.commit();
            return t;
        } catch (final Exception e) {
            try {
                ut.rollback();
            } catch (final SystemException e1) {
                // no-op
            }
            throw new IllegalStateException(e);
        }
    }

    public void clean() {
        inTx(() -> Stream.of("Token", "Notification", "Category", "Post", "User", "Attachment")
                .forEach(table -> entityManager.createQuery("delete from " + table).executeUpdate()));
    }

    public void executeInAdminContext(final Runnable runnable) {
        goTo(null);

        final Home home = getPage(Home.class);
        waitUntil(() -> isDisplayed(home.loginLink));
        home.loginLink.click();

        User created = null;
        try { // we need an admin user, this one is supposed always there
            created = createUser("admin", "admin");
        } catch (final Exception e) {
            // ok already there
        }

        final LoginForm loginForm = getPage(LoginForm.class);
        waitUntil(() -> loginForm.login.isDisplayed());
        loginForm.login(this, "admin", "admin");
        waitUntil(() -> isDisplayed(home.logoutLink));

        try {
            runnable.run();
        } finally {
            home.logoutLink.click();
            waitUntil(() -> isDisplayed(home.loginLink)); // before deleting the user to avoid concurrent issues
            ofNullable(created).ifPresent(u -> deleteUser(u.getId()));
        }
    }

    public void waitUntil(final BooleanSupplier booleanSupplier) {
        if (waitDriver == null) {
            waitDriver = new WebDriverWait(browser(), TimeUnit.MINUTES.toSeconds(5));
        }
        waitDriver.until((Function<? super WebDriver, Boolean>) d -> booleanSupplier.getAsBoolean());
    }

    public void setInputText(final WebElement elt, final String value) {
        elt.clear();
        // workaround for chrome
        IntStream.range(0, value.length()).forEach(i -> elt.sendKeys(Character.toString(value.charAt(i))));
        /*
        // workaround for phantomjs: write twice to let it eat characters the first time
        elt.sendKeys("workaround");
        elt.clear();
        elt.sendKeys(value);
        */
    }

    public static class Home {
        @FindBy(id = "loginLink")
        public WebElement loginLink;

        @FindBy(id = "logoutLink")
        public WebElement logoutLink;
    }

    public static class LoginForm {
        @FindBy(id = "login")
        public WebElement login;

        @FindBy(id = "password")
        public WebElement password;

        @FindBy(id = "loginSubmit")
        public WebElement submit;

        @FindBy(id = "loginErrorMessages")
        public WebElement errors;

        public void login(final RBlog blog, final String user, final String pwd) {
            blog.setInputText(login, user);
            blog.setInputText(password, pwd);
            submit.click();
        }
    }

    // hack for multipart upload, should be in a (deployment) config file but not desired in the binary
    // = by default AttachmentResource doesn't work
    public void withAutoMultipartSupport(final Runnable test) {
        final Context context = Context.class.cast(TomcatHelper.getServer().findServices()[0].getContainer().findChildren()[0].findChildren()[0]);
        context.setAllowCasualMultipartParsing(true);
        try {
            test.run();
        } finally {
            context.setAllowCasualMultipartParsing(false); // default
        }
    }

    //
    // some tomee embedded JUnit integration, it reuses some application composer goodness but just deploy in embedded mode
    //
    // it mainly creates a single container for the whole test suite and bind some useful values to a singleton of RBlog
    // - this is highly inspired from application composer usage.
    //

    @Vetoed
    public static class Rule implements TestRule { // use when you use another runner like Parameterized of JUnit
        private final Object test;

        public Rule(final Object test) {
            this.test = test;
        }

        @Override
        public Statement apply(final Statement base, final Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    Runner.start();
                    Runner.composerInject(test);
                    base.evaluate();
                }
            };
        }
    }

    @Vetoed
    public static class Runner extends BlockJUnit4ClassRunner {
        public Runner(final Class<?> klass) throws InitializationError {
            super(klass);
        }

        private static final AtomicReference<RBlog> APP = new AtomicReference<>();
        private static final AtomicReference<Container> CONTAINER = new AtomicReference<>();
        private static final AtomicReference<Thread> HOOK = new AtomicReference<>();

        public static void close() {
            final Thread hook = HOOK.get();
            if (hook != null) {
                if (HOOK.compareAndSet(hook, null)) {
                    hook.run();
                }
            }
        }

        @Override
        protected List<MethodRule> rules(final Object test) {
            final List<MethodRule> rules = super.rules(test);
            rules.add((base1, method, target) -> new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    start();
                    composerInject(target);
                    base1.evaluate();
                }
            });
            return rules;
        }

        private static void start() throws Exception {
            if (CONTAINER.get() == null) {
                synchronized (RBlog.Runner.class) { // we can't use compareAndSet since we would create 2 containers potentially
                    if (CONTAINER.get() != null) {
                        return;
                    }

                    // setup the container config reading class annotation, using a randome http port and deploying the classpath
                    final Configuration configuration = new Configuration().randomHttpPort();
                    Stream.of(RBlog.class.getAnnotation(ContainerProperties.class).value())
                            .forEach(p -> configuration.property(p.name(), p.value().replace("${http.port}", Integer.toString(configuration.getHttpPort()))));
                    final Container container = new Container(configuration).deployClasspathAsWebApp("rblog", new File("src/main/frontend/dist"));
                    CONTAINER.compareAndSet(null, container);

                    // create app helper and inject basic values
                    final RBlog app = new RBlog();
                    app.base = new URL("http://localhost:" + configuration.getHttpPort() + "/");
                    app.initBase();
                    APP.set(app);
                    composerInject(app);

                    final Thread hook = new Thread() {
                        @Override
                        public void run() {
                            app.clear();
                            container.close();
                            CONTAINER.set(null);
                            APP.set(null);
                            try {
                                Runtime.getRuntime().removeShutdownHook(this);
                            } catch (final Exception e) {
                                // no-op: that's ok at that moment if not called manually
                            }
                        }
                    };
                    HOOK.set(hook);
                    Runtime.getRuntime().addShutdownHook(hook);
                }
            }
        }

        private static void composerInject(final Object target) throws IllegalAccessException { // bridge with app composer
            OWBInjector.inject(WebBeansContext.currentInstance().getBeanManagerImpl(), target, null);

            final Object app = APP.get();
            final Class<?> aClass = target.getClass();
            for (final Field f : aClass.getDeclaredFields()) {
                if (f.isAnnotationPresent(RandomPort.class)) {
                    for (final Field field : app.getClass().getDeclaredFields()) {
                        if (field.getType() == f.getType()) {
                            if (!field.isAccessible()) {
                                field.setAccessible(true);
                            }
                            if (!f.isAccessible()) {
                                f.setAccessible(true);
                            }

                            final Object value = field.get(app);
                            f.set(target, value);
                            break;
                        }
                    }
                } else if (f.isAnnotationPresent(Application.class)) {
                    if (!f.isAccessible()) {
                        f.setAccessible(true);
                    }
                    f.set(target, app);
                }
            }
            final Class<?> superclass = aClass.getSuperclass();
            if (superclass != Object.class) {
                composerInject(superclass);
            }
        }
    }
}
