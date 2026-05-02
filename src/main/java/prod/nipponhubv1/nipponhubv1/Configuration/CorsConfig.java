package prod.nipponhubv1.nipponhubv1.Configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;


/**
 * CORS — dev : tous les origines acceptés.
 * prod : remplacer allowedOrigins par les domaines réels via variable d'env.
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                String origins = allowedOrigins == null || allowedOrigins.isBlank()
                    ? "https://front-end-nipponv1.vercel.app"
                    : allowedOrigins;
                String[] originPatterns = Arrays.stream(origins.split(","))
                    .map(String::trim)
                    .filter(origin -> !origin.isBlank())
                    .toArray(String[]::new);

                registry.addMapping("/**")
                    .allowedOriginPatterns(originPatterns.length == 0 ? new String[] { "*" } : originPatterns)
                    .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
                    .exposedHeaders("Authorization", "Retry-After")
                    .allowCredentials(!Arrays.asList(originPatterns).contains("*"))
                    .maxAge(3600);
            }
        };
    }
}
