package prod.nipponhubv1.nipponhubv1.Configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;
import prod.nipponhubv1.nipponhubv1.Services.OurUserDetailsService;

/**
 * Matrice d'accès NipponHub v1.0 — basée sur les rôles.
 * ─────────────────────────────────────────────────────────────────────────────
 *  PUBLIC            /auth/**  · GET /api/v1/products/**  · GET /api/v1/categories/**
 *                    GET /api/v1/franchises/**  · GET /api/v1/countries/**
 *                    GET /api/v1/cities/**      · GET /api/v1/whatsapp/**
 *
 *  CLIENT+           POST /api/v1/orders/**     · GET /api/v1/orders/my-orders/**
 *                    /api/v1/loyalty/**          · /api/v1/wishlist/**
 *                    GET|PUT /api/v1/users/me
 *
 *  PARTNER+          /api/v1/affiliate/portal/**
 *
 *  ADMIN+            POST|PUT|DELETE /api/v1/products/**
 *                    /api/v1/stocks/**           · /api/v1/flash-sales/**
 *                    /api/v1/orders/**           · /api/v1/pos/**
 *                    GET /api/v1/users/**
 *
 *  OWNER only        /api/v1/owner/**            · /api/v1/config/**
 *                    /api/v1/affiliate/manage/**
 * ─────────────────────────────────────────────────────────────────────────────
 * Fine-grained @PreAuthorize sur les méthodes individuelles pour enforcement additionnel.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final OurUserDetailsService userDetailsService;
    private final JWTAuthFilter         jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Rate limiter avant tout
            .addFilterBefore(new SimpleRateLimitFilter(),
                UsernamePasswordAuthenticationFilter.class)

            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(s ->
                s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth

                // ── PUBLIC ─────────────────────────────────────────────────
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()

                .requestMatchers(HttpMethod.GET,  "/api/v1/products/**").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/v1/categories/**").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/v1/franchises/**").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/v1/countries/**").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/v1/cities/**").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/v1/whatsapp/**").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/v1/config/countries/**").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/v1/config/whatsapp/**").permitAll()

                // ── CLIENT — commandes & profil ────────────────────────────
                .requestMatchers(HttpMethod.POST, "/api/v1/orders").hasAnyRole("CLIENT","ADMIN","OWNER")
                .requestMatchers("/api/v1/orders/my-orders/**").hasAnyRole("CLIENT","ADMIN","OWNER")
                .requestMatchers("/api/v1/loyalty/**").hasAnyRole("CLIENT","ADMIN","OWNER")
                .requestMatchers("/api/v1/wishlist/**").hasAnyRole("CLIENT","ADMIN","OWNER")
                .requestMatchers(HttpMethod.GET,  "/api/v1/users/me").authenticated()
                .requestMatchers(HttpMethod.PUT,  "/api/v1/users/me").authenticated()
                .requestMatchers(HttpMethod.PATCH,"/api/v1/users/me/avatar").authenticated()
                .requestMatchers("/api/v1/users/me/addresses/**").authenticated()

                // ── PARTNER ────────────────────────────────────────────────
                .requestMatchers("/api/v1/affiliate/portal/**")
                    .hasAnyRole("PARTNER","ADMIN","OWNER")

                // ── ADMIN ──────────────────────────────────────────────────
                .requestMatchers(HttpMethod.POST,   "/api/v1/products/**")
                    .hasAnyRole("ADMIN","OWNER")
                .requestMatchers(HttpMethod.PUT,    "/api/v1/products/**")
                    .hasAnyRole("ADMIN","OWNER")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**")
                    .hasAnyRole("ADMIN","OWNER")
                .requestMatchers("/api/v1/stocks/**").hasAnyRole("ADMIN","OWNER")
                .requestMatchers("/api/v1/flash-sales/**").hasAnyRole("ADMIN","OWNER")
                .requestMatchers("/api/v1/orders/**").hasAnyRole("ADMIN","OWNER")
                .requestMatchers("/api/v1/pos/**").hasAnyRole("ADMIN","OWNER")
                .requestMatchers(HttpMethod.GET, "/api/v1/users/**")
                    .hasAnyRole("ADMIN","OWNER")
                .requestMatchers(HttpMethod.PUT, "/api/v1/users/**")
                    .hasAnyRole("ADMIN","OWNER")

                // ── OWNER uniquement ───────────────────────────────────────
                .requestMatchers("/api/v1/owner/**").hasRole("OWNER")
                .requestMatchers("/api/v1/config/**").hasAnyRole("OWNER","ADMIN")
                .requestMatchers("/api/v1/affiliate/manage/**").hasRole("OWNER")

                .anyRequest().authenticated()
            )

            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider =
            new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);  // coût 12 (production)
    }

    @Bean
    public AuthenticationManager authenticationManager(
        AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
