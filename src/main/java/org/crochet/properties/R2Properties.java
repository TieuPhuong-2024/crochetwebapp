package org.crochet.properties;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.stream.Stream;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "r2")
public class R2Properties {
    private String endpoint = "";
    private String accessKey = "";
    private String secretKey = "";
    private String bucketName = "";
    private String cdnUrl = "";
    @Pattern(regexp = "development|production")
    private String environment = "development";

    public void validateEnvironment() {
        if (!"development".equals(environment) && !"production".equals(environment)) {
            throw new IllegalStateException("R2_ENVIRONMENT must be development or production");
        }
    }

    public boolean isConfigured() {
        validateEnvironment();
        return Stream.of(endpoint, accessKey, secretKey, bucketName, cdnUrl)
                .allMatch(value -> value != null && !value.isBlank());
    }

    public void requireConfigured() {
        if (!isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "R2 storage is not configured");
        }
        checkedUri(endpoint);
        checkedUri(cdnUrl);
        if (!bucketName.matches("[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]")) {
            throw new IllegalStateException("Invalid R2 bucket configuration");
        }
    }

    public URI cdnUri() {
        return cdnUrl == null || cdnUrl.isBlank() ? null : checkedUri(cdnUrl);
    }

    public String publicUrl(String key) {
        return cdnUrl.replaceAll("/+$", "") + "/" + key;
    }

    public void validateScope(String bucket, String key) {
        validateEnvironment();
        if (bucketName == null || bucketName.isBlank() || !bucketName.equals(bucket)
                || key == null || !key.startsWith(environment + "/")
                || !key.matches("(?:development|production)/[a-z0-9][a-z0-9-]{0,39}/[a-z0-9][a-z0-9-]{0,39}/[0-9a-f-]{36}\\.[a-z0-9]+")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Upload storage scope does not match");
        }
    }

    private URI checkedUri(String value) {
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || uri.getRawPath().contains("%") || uri.getRawPath().contains("..")) {
                throw new IllegalArgumentException();
            }
            return uri;
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Invalid R2 URL configuration");
        }
    }
}
