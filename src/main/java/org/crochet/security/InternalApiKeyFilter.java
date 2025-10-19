package org.crochet.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static org.springframework.util.StringUtils.hasText;

/**
 * Filter to validate API key for internal endpoints
 */
@Slf4j
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    @Value("${internal.api.key:internal-api-key}")
    private String internalApiKey;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // Only apply to internal endpoints
        if (requestURI.startsWith("/api/v1/internal/")) {
            String apiKey = request.getHeader("X-Internal-Api-Key");

            if (!hasText(apiKey) || !apiKey.equals(internalApiKey)) {
                log.warn("Invalid or missing API key for internal endpoint: {}", requestURI);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"error\": \"Unauthorized\", \"message\": \"Invalid or missing API key\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
