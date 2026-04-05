package com.digitaltwin.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory token-bucket rate limiter per IP address.
 * Allows up to MAX_REQUESTS_PER_WINDOW requests in a sliding WINDOW_MS window.
 * This is a basic implementation suitable for single-instance deployments;
 * for multi-instance deployments a distributed store (Redis) would be needed.
 */
@Component
@Order(2)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final int MAX_REQUESTS_PER_WINDOW = 200;
    private static final long WINDOW_MS = 60_000L; // 1 minute

    // Map from IP -> [requestCount, windowStartMs]
    private final ConcurrentHashMap<String, long[]> counters = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        // Skip actuator and OPTIONS
        String path = request.getRequestURI();
        if (path.startsWith("/actuator") || "OPTIONS".equals(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        long now = System.currentTimeMillis();

        long[] bucket = counters.compute(clientIp, (ip, existing) -> {
            if (existing == null || now - existing[1] > WINDOW_MS) {
                // New window
                return new long[]{1L, now};
            }
            existing[0]++;
            return existing;
        });

        long requestCount = bucket[0];

        if (requestCount > MAX_REQUESTS_PER_WINDOW) {
            log.warn("Rate limit exceeded for IP: {}", clientIp);
            response.setStatus(429); // Too Many Requests
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Try again later.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // Take the first IP if there are multiple (proxy chain)
            int commaIdx = forwarded.indexOf(',');
            return commaIdx >= 0 ? forwarded.substring(0, commaIdx).trim() : forwarded.trim();
        }
        return request.getRemoteAddr();
    }
}
