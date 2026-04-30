package prod.nipponhubv1.nipponhubv1.Services;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Function;
import java.util.Date;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import prod.nipponhubv1.nipponhubv1.Models.OurUser;
import prod.nipponhubv1.nipponhubv1.Models.Enums.Role;

/**
 * Gestion des tokens JWT.
 * - Access token  : durée courte (configurable, défaut 1h)
 * - Refresh token : durée longue (configurable, défaut 7j)
 * Le rôle de l'utilisateur est embarqué dans les claims pour éviter
 * un appel DB supplémentaire à chaque vérification.
 */
@Component
@Slf4j
public class JWTUtils {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "token_type";
    private static final String TYPE_ACCESS  = "access";
    private static final String TYPE_REFRESH = "refresh";

    @Value("${app.jwt.secret}")
    private String secretString;

    @Value("${app.jwt.access-expiration:3600000}")   // 1h défaut
    private long accessExpiration;

    @Value("${app.jwt.refresh-expiration:604800000}") // 7j défaut
    private long refreshExpiration;

    private SecretKey key;

    @PostConstruct
    public void initKey() {
        if (secretString == null || secretString.isBlank()) {
            throw new IllegalStateException(
                "app.jwt.secret non configuré. Définissez JWT_SECRET dans les variables d'environnement."
            );
        }
        byte[] keyBytes = Base64.getDecoder().decode(
            secretString.getBytes(StandardCharsets.UTF_8)
        );
        this.key = new SecretKeySpec(keyBytes, "HmacSHA256");
        log.info("✓ JWT initialisé — access={}ms refresh={}ms", accessExpiration, refreshExpiration);
    }

    // ── Génération ───────────────────────────────────────────────────────────

    public String generateAccessToken(OurUser user) {
        return buildToken(user, accessExpiration, TYPE_ACCESS);
    }

    public String generateRefreshToken(OurUser user) {
        return buildToken(user, refreshExpiration, TYPE_REFRESH);
    }

    private String buildToken(OurUser user, long expMs, String type) {
        return Jwts.builder()
            .subject(user.getEmail())
            .claim(CLAIM_ROLE, user.getRole().name())
            .claim(CLAIM_TYPE, type)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expMs))
            .signWith(key)
            .compact();
    }

    // ── Extraction ───────────────────────────────────────────────────────────

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Role extractRole(String token) {
        String roleStr = extractClaim(token, c -> c.get(CLAIM_ROLE, String.class));
        return Role.valueOf(roleStr);
    }

    public boolean isAccessToken(String token) {
        return TYPE_ACCESS.equals(extractClaim(token, c -> c.get(CLAIM_TYPE, String.class)));
    }

    public boolean isRefreshToken(String token) {
        return TYPE_REFRESH.equals(extractClaim(token, c -> c.get(CLAIM_TYPE, String.class)));
    }

    // ── Validation ───────────────────────────────────────────────────────────

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String email = extractEmail(token);
            return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception e) {
            log.warn("Token invalide : {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    public long getAccessExpirationSeconds() {
        return accessExpiration / 1000;
    }

    // ── Interne ──────────────────────────────────────────────────────────────

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
            .verifyWith(key).build()
            .parseSignedClaims(token)
            .getPayload();
        return resolver.apply(claims);
    }
}
