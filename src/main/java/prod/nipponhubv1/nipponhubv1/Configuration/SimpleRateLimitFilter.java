package prod.nipponhubv1.nipponhubv1.Configuration;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtre de limitation de débit sur /auth/** uniquement.
 * Amélioration vs original :
 *  - Fenêtre glissante (Sliding Window) au lieu de fenêtre fixe
 *  - Nettoyage automatique des entrées expirées toutes les 5 minutes
 *  - Réponse JSON avec Retry-After header
 */
public class SimpleRateLimitFilter extends OncePerRequestFilter {

    private static final int    MAX_REQUESTS = 5;
    private static final long   WINDOW_MS    = 60_000L;          // 1 minute
    private static final long   CLEANUP_MS   = 300_000L;         // 5 minutes

    private final ConcurrentHashMap<String, Deque<Long>> requestTimestamps
        = new ConcurrentHashMap<>();
    private volatile long lastCleanup = System.currentTimeMillis();

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain)
        throws ServletException, IOException {

        if (!req.getRequestURI().startsWith("/auth")) {
            chain.doFilter(req, res);
            return;
        }

        cleanupIfNeeded();

        String ip      = resolveClientIp(req);
        long   now     = System.currentTimeMillis();
        long   windowStart = now - WINDOW_MS;

        Deque<Long> timestamps = requestTimestamps
            .computeIfAbsent(ip, k -> new ArrayDeque<>());

        synchronized (timestamps) {
            // Retirer les timestamps hors fenêtre
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= MAX_REQUESTS) {
                long retryAfter = (timestamps.peekFirst() + WINDOW_MS - now) / 1000;
                res.setStatus(429);
                res.setContentType("application/json");
                res.setHeader("Retry-After", String.valueOf(retryAfter));
                res.getWriter().write(
                    "{\"error\":\"Trop de tentatives. Réessayez dans " + retryAfter + " secondes.\"}"
                );
                return;
            }

            timestamps.addLast(now);
        }

        chain.doFilter(req, res);
    }

    private void cleanupIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCleanup > CLEANUP_MS) {
            lastCleanup = now;
            long cutoff = now - WINDOW_MS;
            requestTimestamps.entrySet().removeIf(e -> {
                synchronized (e.getValue()) {
                    return e.getValue().isEmpty()
                        || e.getValue().peekLast() < cutoff;
                }
            });
        }
    }

    private String resolveClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
