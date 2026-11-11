package org.crochet.service;

import org.crochet.model.UploadAsset;
import org.crochet.model.User;
import org.crochet.properties.R2Properties;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

final class UploadTestSupport {
    static final Instant NOW = Instant.parse("2026-11-11T00:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final String KEY = "development/blogs/images/00000000-0000-0000-0000-000000000001.png";
    static final String CDN = "https://cdn.example.test";

    static R2Properties properties() {
        R2Properties properties = new R2Properties();
        properties.setEnvironment("development");
        properties.setBucketName("unit-test-bucket");
        properties.setCdnUrl(CDN);
        properties.setEndpoint("https://storage.example.test");
        properties.setAccessKey("test-only-key");
        properties.setSecretKey("test-only-secret");
        return properties;
    }

    static UploadAsset asset() {
        return asset(KEY, NOW.plusSeconds(86400));
    }

    static UploadAsset asset(String key, Instant expiry) {
        return new UploadAsset(key, "development", "unit-test-bucket", "owner", "image/png", 100,
                expiry.minusSeconds(86400), expiry);
    }

    static void authenticate(String id, String role) {
        User user = new User();
        user.setId(id);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null,
                List.of(new SimpleGrantedAuthority(role))));
    }
}
