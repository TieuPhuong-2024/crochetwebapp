package org.crochet.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Brevo (formerly Sendinblue) transaction email settings.
 * See <a href="https://developers.brevo.com/reference/sendtransacemail">POST /v3/smtp/email</a>.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "brevo")
public class BrevoProperties {

    /**
     * Brevo API v3 key. Sent in the {@code api-key} header.
     */
    private String apiKey;

    /**
     * Brevo API base URL.
     */
    private String baseUrl = "https://api.brevo.com/v3";

    private final Sender sender = new Sender();

    @Getter
    @Setter
    public static final class Sender {
        private String email;
        private String name;
    }
}
