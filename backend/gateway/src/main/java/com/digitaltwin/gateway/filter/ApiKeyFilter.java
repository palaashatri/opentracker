package com.digitaltwin.gateway.filter;

import com.digitaltwin.gateway.config.GatewayProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1)
public class ApiKeyFilter extends OncePerRequestFilter {

    private final GatewayProperties properties;

    public ApiKeyFilter(GatewayProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip actuator endpoints and preflight OPTIONS requests
        if (path.startsWith("/actuator") || "OPTIONS".equals(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        // Only enforce auth on /api/** paths
        if (!path.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader("X-Api-Key");
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = request.getParameter("api_key");
        }

        // Dev mode: if configured key is "dev-key", allow all requests through
        // Otherwise validate the provided key matches the configured key
        if ("dev-key".equals(properties.getApiKey()) || properties.getApiKey().equals(apiKey)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Missing or invalid X-Api-Key header\"}");
        }
    }
}
