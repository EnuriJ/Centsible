package com.centsible.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory, per-IP rate limiter for the unauthenticated auth endpoints
 * (login/signup) - the obvious target for brute-force password guessing or signup spam,
 * since nothing else in the API is reachable without a token.
 *
 * Clients are keyed on request.getRemoteAddr() only - the X-Forwarded-For header is
 * client-controlled, so trusting it would let anyone dodge the limit by sending a fresh
 * fake value each request. Behind a hosting platform's proxy, the production profile sets
 * server.forward-headers-strategy=native so Tomcat resolves the real client address from
 * the proxy's forwarded header (only when the request actually arrived via a trusted proxy).
 *
 * Fixed-window counter, single JVM instance. That's intentionally simple: fine for a
 * single deployed instance, which is all this project needs. A multi-instance deployment
 * would need a shared store (e.g. Redis) instead, since each instance would otherwise
 * count independently.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_WINDOW = 10;
    private static final long WINDOW_MILLIS = 60_000; // 1 minute

    private static final class Window {
        final AtomicInteger count = new AtomicInteger(0);
        volatile long windowStart = System.currentTimeMillis();
    }

    private final ConcurrentHashMap<String, Window> requestCounts = new ConcurrentHashMap<>();
    private volatile long lastCleanup = System.currentTimeMillis();

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean isAuthEndpoint = path.equals("/api/auth/login") || path.equals("/api/auth/signup");
        return !isAuthEndpoint || !"POST".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        long now = System.currentTimeMillis();
        evictStaleEntries(now);

        String clientIp = request.getRemoteAddr();
        Window window = requestCounts.computeIfAbsent(clientIp, ip -> new Window());

        synchronized (window) {
            if (now - window.windowStart > WINDOW_MILLIS) {
                window.windowStart = now;
                window.count.set(0);
            }
        }

        if (window.count.incrementAndGet() > MAX_REQUESTS_PER_WINDOW) {
            response.setStatus(429); // Servlet API has no named constant for 429
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests. Please try again in a minute.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    // Drop entries whose window has expired so the map can't grow without bound as new
    // client addresses appear. Runs at most once per window.
    private void evictStaleEntries(long now) {
        if (now - lastCleanup < WINDOW_MILLIS) {
            return;
        }
        lastCleanup = now;
        requestCounts.entrySet().removeIf(entry -> now - entry.getValue().windowStart > WINDOW_MILLIS);
    }
}
