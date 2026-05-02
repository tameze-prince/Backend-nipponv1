package prod.nipponhubv1.nipponhubv1.Configuration;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads local .env values for development while letting real platform
 * environment variables win in production.
 */
public class DotenvConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext ctx) {
        try {
            Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .ignoreIfMalformed()
                .load();

            Map<String, Object> props = new HashMap<>();
            dotenv.entries().forEach(e -> {
                if (System.getenv(e.getKey()) == null) {
                    props.put(e.getKey(), e.getValue());
                }
            });
            addDatabaseUrlProperties(props);

            ctx.getEnvironment()
                .getPropertySources()
                .addFirst(new MapPropertySource("dotenvProperties", props));
        } catch (Exception e) {
            System.out.println("[DotenvConfig] .env not loaded: " + e.getMessage());
        }
    }

    private void addDatabaseUrlProperties(Map<String, Object> props) {
        if (hasValue("DB_URL", props)) {
            return;
        }

        String databaseUrl = getValue("DATABASE_URL", props);
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }

        if (databaseUrl.startsWith("jdbc:postgresql://")) {
            props.put("spring.datasource.url", databaseUrl);
            return;
        }

        try {
            URI uri = URI.create(databaseUrl);
            if (!"postgres".equals(uri.getScheme()) && !"postgresql".equals(uri.getScheme())) {
                return;
            }

            String jdbcUrl = "jdbc:postgresql://" + uri.getHost()
                + (uri.getPort() > -1 ? ":" + uri.getPort() : "")
                + uri.getPath();
            if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
                jdbcUrl += "?" + uri.getQuery();
            }

            props.put("spring.datasource.url", jdbcUrl);

            String userInfo = uri.getUserInfo();
            if (userInfo != null && !userInfo.isBlank()) {
                String[] credentials = userInfo.split(":", 2);
                props.putIfAbsent("spring.datasource.username", decode(credentials[0]));
                if (credentials.length > 1) {
                    props.putIfAbsent("spring.datasource.password", decode(credentials[1]));
                }
            }
        } catch (IllegalArgumentException e) {
            System.out.println("[DotenvConfig] Ignoring invalid DATABASE_URL");
        }
    }

    private boolean hasValue(String key, Map<String, Object> props) {
        String value = getValue(key, props);
        return value != null && !value.isBlank();
    }

    private String getValue(String key, Map<String, Object> props) {
        String envValue = System.getenv(key);
        if (envValue != null) {
            return envValue;
        }

        Object propValue = props.get(key);
        return propValue == null ? null : propValue.toString();
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
