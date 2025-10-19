package org.crochet.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.crochet.payload.response.BlogPostResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

/**
 * REST client for communicating with Blog Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BlogServiceClient {

    private final RestClient restClient;

    @Value("${blog-service.url:http://localhost:8083}")
    private String blogServiceUrl;

    @Value("${internal.api.key:internal-api-key}")
    private String internalApiKey;

    /**
     * Get limited blog posts for the home page
     */
    public List<BlogPostResponse> getLimitedBlogPosts() {
        try {
            return restClient.get()
                    .uri(blogServiceUrl + "/api/v1/blogs/limited")
                    .header("X-Internal-Api-Key", internalApiKey)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<BlogPostResponse>>() {});
        } catch (Exception e) {
            log.error("Failed to get limited blog posts from blog service: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
