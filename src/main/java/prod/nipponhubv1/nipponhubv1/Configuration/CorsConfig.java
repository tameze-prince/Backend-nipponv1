package prod.nipponhubv1.nipponhubv1.Configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.springframework.beans.factory.annotation.Value;


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
                    ? "https://front-end-nipponv1.vercel.app/"
                    : allowedOrigins;
                registry.addMapping("/**")
                    .allowedOriginPatterns(origins.equals("*") ? "*" : origins)
                    .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
                    .exposedHeaders("Authorization", "Retry-After")
                    .allowCredentials(!origins.equals("*"))
                    .maxAge(3600);
            }
        };
    }
}
