package org.crochet.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.crochet.service.JwtTokenService;
import org.crochet.util.TokenUtils;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static org.springframework.util.StringUtils.hasText;

/**
 * TokenAuthenticationFilter class
 */
@Slf4j
@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenService jwtTokenService;
    private final CustomUserDetailsService customUserDetailsService;

    public TokenAuthenticationFilter(JwtTokenService jwtTokenService,
                                     CustomUserDetailsService customUserDetailsService) {
        this.jwtTokenService = jwtTokenService;
        this.customUserDetailsService = customUserDetailsService;
    }

    /**
     * Performs the filtering logic for the authentication process.
     *
     * @param request     The HttpServletRequest object.
     * @param response    The HttpServletResponse object.
     * @param filterChain The FilterChain object for invoking the next filter in the chain.
     * @throws ServletException If an exception occurs during the filtering process.
     * @throws IOException      If an I/O exception occurs.
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // Get jwtToken
            var jwtToken = TokenUtils.getJwtFromAuthorizationHeader(request);
            // Check if the JWT exists and is valid
            if (hasText(jwtToken)) {
                if (jwtTokenService.validateToken(jwtToken)) {
                    String username = jwtTokenService.extractUsername(jwtToken);
                    // Load the user details by email
                    UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
                    if (jwtTokenService.isTokenValid(jwtToken, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                        authToken.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context for path: {}", request.getRequestURI(), ex);
        }

        // Continue the filter chain
        filterChain.doFilter(request, response);
    }
}
