package org.crochet.service.impl;

import org.crochet.properties.BrevoProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class BrevoEmailServiceTest {

    private static final String BASE_URL = "https://api.brevo.com/v3";

    private MockRestServiceServer server;
    private BrevoEmailService brevoEmailService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();

        BrevoProperties brevoProperties = new BrevoProperties();
        brevoProperties.setApiKey("test-api-key");
        brevoProperties.setBaseUrl(BASE_URL);
        brevoProperties.getSender().setEmail("thamphuong.crochet@gmail.com");
        brevoProperties.getSender().setName("Little Crochet");

        RestClient restClient = builder
                .baseUrl(BASE_URL)
                .defaultHeader("api-key", brevoProperties.getApiKey())
                .build();

        brevoEmailService = new BrevoEmailService(restClient, brevoProperties);
    }

    @Test
    void sendPostsTransactionEmailPayload() {
        server.expect(requestTo(BASE_URL + "/smtp/email"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "test-api-key"))
                .andExpect(jsonPath("$.sender.email").value("thamphuong.crochet@gmail.com"))
                .andExpect(jsonPath("$.sender.name").value("Little Crochet"))
                .andExpect(jsonPath("$.to[0].email").value("buyer@example.com"))
                .andExpect(jsonPath("$.subject").value("Confirm your email"))
                .andExpect(jsonPath("$.htmlContent").value("<p>hello</p>"))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"messageId\":\"<20260101.1@brevo>\"}"));

        assertThatCode(() -> brevoEmailService.send("buyer@example.com", "Confirm your email", "<p>hello</p>"))
                .doesNotThrowAnyException();

        server.verify();
    }

    @Test
    void sendThrowsIllegalStateExceptionWhenBrevoRejectsRequest() {
        server.expect(requestTo(BASE_URL + "/smtp/email"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"invalid_parameter\",\"message\":\"sender is not valid\"}"));

        assertThatThrownBy(() -> brevoEmailService.send("buyer@example.com", "Confirm your email", "<p>hello</p>"))
                .isInstanceOf(org.crochet.exception.IllegalStateException.class);

        server.verify();
    }

    @Test
    void sendThrowsIllegalStateExceptionWhenBrevoIsUnavailable() {
        server.expect(requestTo(BASE_URL + "/smtp/email"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> brevoEmailService.send("buyer@example.com", "Confirm your email", "<p>hello</p>"))
                .isInstanceOf(org.crochet.exception.IllegalStateException.class);

        server.verify();
    }
}
