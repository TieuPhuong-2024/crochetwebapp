package org.crochet.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;
import org.crochet.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@UtilityClass
public class SecurityUtils {

    /**
     * Gets the currently authenticated user from the security context
     *
     * @return The authenticated User object, or null if no user is authenticated
     */
    public User getCurrentUser() {
        Authentication authentication = getAuthentication();
        if (!isValidAuthentication(authentication)) {
            return null;
        }
        return (User) authentication.getPrincipal();
    }

    private Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private boolean isValidAuthentication(Authentication authentication) {
        return authentication != null 
               && authentication.isAuthenticated()
               && !authentication.getPrincipal().equals("anonymousUser");
    }

    public boolean hasRole(String role) {
        Authentication authentication = getAuthentication();
        if (!isValidAuthentication(authentication)) {
            return false;
        }
        return authentication.getAuthorities().contains(new SimpleGrantedAuthority(role));
    }

    public String getClientIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }

        // Nếu có nhiều IP (do proxy), lấy IP đầu tiên
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }

        return ipAddress;
    }
}
