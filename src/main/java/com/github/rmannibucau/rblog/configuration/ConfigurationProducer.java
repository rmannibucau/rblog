package com.github.rmannibucau.rblog.configuration;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@ApplicationScoped
public class ConfigurationProducer {
    private static final String DECRYPT_PREFIX = "decrypt:";

    private final Map<String, String> values = new HashMap<>();

    @Any
    @Inject
    private Instance<Decrypter> decrypters;

    @Inject
    private DefaultDecrypter defaultDecrypter;

    @PostConstruct
    private void read() {
        // classloader
        ofNullable(Thread.currentThread().getContextClassLoader().getResourceAsStream("rblog.properties")).ifPresent(r -> {
            final Properties properties = new Properties();
            try {
                properties.load(r);
            } catch (final IOException e) {
                throw new IllegalArgumentException(e);
            } finally {
                try {
                    r.close();
                } catch (IOException e) {
                    // no-op
                }
            }
            values.putAll(Map.class.cast(properties));
        });
        // files
        Stream.of("rblog.base", "openejb.base")
            .map(System::getProperty).filter(v -> v != null).map(File::new).filter(File::isDirectory)
            .map(d -> new File(d, "conf/rblog.properties")).filter(File::isFile)
            .forEach(config -> {
                try (final InputStream is = new FileInputStream(config)) {
                    final Properties properties = new Properties();
                    properties.load(is);
                    values.putAll(Map.class.cast(properties));
                } catch (final IOException e) {
                    throw new IllegalArgumentException(e);
                }
            });
        // system properties
        values.putAll(System.getProperties().stringPropertyNames().stream().collect(toMap(identity(), System::getProperty)));
    }

    @Produces
    @Configuration("")
    public String string(final InjectionPoint ip) {
        final Configuration configuration = ip.getAnnotated().getAnnotation(Configuration.class);
        final String val = configuration.value();
        final int colon = val.indexOf(':');
        final boolean hasDefault = val.startsWith("${") && val.endsWith("}") && colon > 0;
        final String key = hasDefault ? val.substring("${".length(), hasDefault ? colon : val.length() - 1) : val;
        return decryptIfNeeded(ofNullable(values.get(key)).orElseGet(() -> hasDefault ? val.substring(colon + 1, val.length() - 1) : null));
    }

    @Produces
    @Configuration("")
    public Integer integer(final InjectionPoint ip) {
        return ofNullable(string(ip)).map(Integer::parseInt).orElse(0);
    }

    @Produces
    @Configuration("")
    public Boolean bool(final InjectionPoint ip) {
        return ofNullable(string(ip)).map(Boolean::parseBoolean).orElse(false);
    }

    private String decryptIfNeeded(final String s) {
        return ofNullable(s).filter(v -> v != null && v.startsWith(DECRYPT_PREFIX)).map(this::doDecrypt).orElse(s);
    }

    private String doDecrypt(final String s) {
        final String config = s.substring(DECRYPT_PREFIX.length());
        final int colon = config.indexOf(':');
        if (colon < 0) {
            return defaultDecrypter.decrypt(config);
        }

        final String pref = config.substring(0, colon);

        // this method is slow for runtime, while we use app scoped services it is ok but if we stop doing it
        // we should cache decrypters
        return StreamSupport.stream(decrypters.spliterator(), false)
            .filter(d -> pref.equals(d.prefix()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No decrypter for algorithm: " + pref))
            .decrypt(config.substring(colon + 1, config.length()));
    }

    public interface Decrypter {
        // yes some people will say char[] is more secured
        // if so please ask them to proove it, you'll get time to be retired
        // before they do it right
        String decrypt(String val);

        String prefix();
    }
}
