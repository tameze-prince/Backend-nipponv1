package prod.nipponhubv1.nipponhubv1.Configuration;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Charge le fichier .env à la racine du projet et injecte
 * toutes les variables comme propriétés Spring Boot.
 *
 * Fonctionne en dev (avec .env) et en production
 * (les vraies variables d'environnement système priment toujours).
 */
public class DotenvConfig
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

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

            ctx.getEnvironment()
               .getPropertySources()
               .addFirst(new MapPropertySource("dotenvProperties", props));

        } catch (Exception e) {
            System.out.println("[DotenvConfig] .env non chargé : " + e.getMessage());
        }
    }
}
