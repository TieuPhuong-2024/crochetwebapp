package org.crochet.service.impl;

import org.crochet.config.BrevoConfig;
import org.crochet.properties.BrevoProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Opt-in smoke test that sends one real transaction email through the live Brevo API,
 * using the same beans as production ({@link BrevoConfig} + {@link BrevoEmailService}).
 *
 * <p>Run locally with:</p>
 * <pre>
 * BREVO_LIVE_TEST=true BREVO_API_KEY=xkeysib-... BREVO_SENDER_EMAIL=verified@example.com \
 *   ./gradlew test --tests '*BrevoLiveSmokeTest*'
 * </pre>
 *
 * <p>Never enabled in CI: it would send a real email on every build.</p>
 */
@EnabledIfEnvironmentVariable(named = "BREVO_LIVE_TEST", matches = "true")
class BrevoLiveSmokeTest {

    @Test
    void sendsRealEmailThroughBrevoRestApi() {
        String apiKey = System.getenv("BREVO_API_KEY");
        String senderEmail = System.getenv("BREVO_SENDER_EMAIL");
        String recipient = System.getenv().getOrDefault("BREVO_TEST_RECIPIENT", senderEmail);
        assertThat(apiKey).as("BREVO_API_KEY").isNotBlank();
        assertThat(senderEmail).as("BREVO_SENDER_EMAIL").isNotBlank();

        BrevoProperties brevoProperties = new BrevoProperties();
        brevoProperties.setApiKey(apiKey);
        brevoProperties.getSender().setEmail(senderEmail);
        brevoProperties.getSender().setName(System.getenv().getOrDefault("BREVO_SENDER_NAME", "Little Crochet"));

        RestClient restClient = new BrevoConfig().brevoRestClient(brevoProperties, RestClient.builder());
        BrevoEmailService brevoEmailService = new BrevoEmailService(restClient, brevoProperties);

        assertThatCode(() -> brevoEmailService.send(recipient,
                "Crochet webapp - Brevo smoke test",
                "<p>Brevo REST API v3 integration smoke test.</p>"))
                .doesNotThrowAnyException();
    }
}
