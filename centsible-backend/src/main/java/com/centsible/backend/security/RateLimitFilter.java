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

        String clientIp = resolveClientIp(request);
        Window window = requestCounts.computeIfAbsent(clientIp, ip -> new Window());

        long now = System.currentTimeMillis();
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

    // Most hosting platforms (Render, Railway, etc.) sit behind a proxy/load balancer, so
    // prefer X-Forwarded-For when present; fall back to the direct remote address for
    // local dev and tests.
    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
