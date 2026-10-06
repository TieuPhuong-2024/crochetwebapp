package org.crochet.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.crochet.enums.ResultCode;
import org.crochet.exception.IllegalStateException;
import org.crochet.properties.BrevoProperties;
import org.crochet.service.EmailSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Sends transaction email through the Brevo REST API v3 ({@code POST /smtp/email}).
 * Active when {@code app.email.provider=brevo} (default). Set {@code app.email.provider=smtp}
 * to fall back to {@link EmailService}.
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "app.email", name = "provider", havingValue = "brevo", matchIfMissing = true)
public class BrevoEmailService implements EmailSender {

    private static final String SEND_EMAIL_URI = "/smtp/email";

    private final RestClient brevoRestClient;
    private final BrevoProperties brevoProperties;

    public BrevoEmailService(RestClient brevoRestClient, BrevoProperties brevoProperties) {
        this.brevoRestClient = brevoRestClient;
        this.brevoProperties = brevoProperties;
    }

    /**
     * Send email
     *
     * @param to      user's email
     * @param subject subject
     * @param content html content
     * @throws IllegalStateException failed to send email
     */
    @Override
    @Async
    public void send(String to, String subject, String content) {
        BrevoEmailRequest request = buildRequest(to, subject, content);
        try {
            brevoRestClient.post()
                    .uri(SEND_EMAIL_URI)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("{} to={}", ResultCode.MSG_FAILED_SEND_EMAIL.message(), to, e);
            throw new IllegalStateException(ResultCode.MSG_FAILED_SEND_EMAIL.message(),
                    ResultCode.MSG_FAILED_SEND_EMAIL.code());
        }
    }

    private BrevoEmailRequest buildRequest(String to, String subject, String content) {
        BrevoProperties.Sender sender = brevoProperties.getSender();
        if (sender == null || !StringUtils.hasText(sender.getEmail())) {
            log.error("brevo.sender.email is not set. Set BREVO_SENDER_EMAIL before sending email.");
            throw new IllegalStateException(ResultCode.MSG_FAILED_SEND_EMAIL.message(),
                    ResultCode.MSG_FAILED_SEND_EMAIL.code());
        }
        return new BrevoEmailRequest(
                new BrevoEmailRequest.Sender(sender.getName(), sender.getEmail()),
                List.of(new BrevoEmailRequest.Recipient(to)),
                subject,
                content);
    }

    /**
     * Payload of {@code POST /v3/smtp/email}.
     */
    public record BrevoEmailRequest(Sender sender, List<Recipient> to, String subject, String htmlContent) {
        public record Sender(String name, String email) {
        }

        public record Recipient(String email) {
        }
    }
}
