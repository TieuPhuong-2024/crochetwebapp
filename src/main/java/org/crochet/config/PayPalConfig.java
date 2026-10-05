package org.crochet.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.paypal.sdk.Environment;
import com.paypal.sdk.PaypalServerSdkClient;
import com.paypal.sdk.authentication.ClientCredentialsAuthModel;

@Configuration
@Slf4j
public class PayPalConfig {

    @Value("${paypal.client-id}")
    private String clientId;

    @Value("${paypal.client-secret}")
    private String clientSecret;

    @Value("${paypal.mode:sandbox}")
    private String mode;

    @Bean
    public PaypalServerSdkClient payPalHttpClient() {
        log.info("Initializing PayPal SDK with mode: {}", mode);
        Environment environment;
        if ("live".equalsIgnoreCase(mode)) {
            environment = Environment.PRODUCTION;
        } else {
            environment = Environment.SANDBOX;
        }
        
        return new PaypalServerSdkClient.Builder()
            .clientCredentialsAuth(new ClientCredentialsAuthModel.Builder(clientId, clientSecret).build())
            .environment(environment)
            .build();
    }
}
