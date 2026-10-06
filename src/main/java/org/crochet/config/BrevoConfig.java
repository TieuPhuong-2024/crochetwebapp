package org.crochet.config;

import lombok.extern.slf4j.Slf4j;
import org.crochet.properties.BrevoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Objects;

@Configuration
@Slf4j
public class BrevoConfig {

    /**
     * Header Brevo expects for API v3 authentication.
     */
    public static final String API_KEY_HEADER = "api-key";

    @Bean
    public RestClient brevoRestClient(BrevoProperties brevoProperties, RestClient.Builder builder) {
        if (!StringUtils.hasText(brevoProperties.getApiKey())) {
            log.warn("brevo.api-key is not set. Set BREVO_API_KEY, otherwise Brevo email requests will fail.");
        }

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        String baseUrl = StringUtils.hasText(brevoProperties.getBaseUrl())
                ? brevoProperties.getBaseUrl()
                : "https://api.brevo.com/v3";

        return builder.clone()
                .baseUrl(baseUrl)
                .defaultHeader(API_KEY_HEADER, Objects.requireNonNullElse(brevoProperties.getApiKey(), ""))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(requestFactory)
                .build();
    }
}
