package org.crochet.config;

import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.Nats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.time.Duration;

@Slf4j
@Configuration
public class NatsConfig {

    @Value("${nats.servers}")
    private String natsServers;

    @Value("${nats.connection-name}")
    private String connectionName;

    @Bean
    public SSLContext sslContext() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");

        // Create a trust manager that trusts all certificates
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    // Trust all client certificates
                }
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    // Trust all server certificates
                }
            }
        };

        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        return sslContext;
    }

    @Bean
    public Connection natsConnection(SSLContext sslContext) throws Exception {
        log.info("Connecting to NATS server: {}", natsServers);

        io.nats.client.Options options = new io.nats.client.Options.Builder()
                .server(natsServers)
                .connectionName(connectionName)
                .connectionTimeout(Duration.ofSeconds(10))
                .reconnectWait(Duration.ofSeconds(2))
                .maxReconnects(-1) // Unlimited reconnects
                .pingInterval(Duration.ofSeconds(20))
                .reconnectBufferSize(1024 * 1024 * 8) // 8MB buffer
                // .sslContext(sslContext) // Use custom SSL context
                .build();

        try {
            Connection connection = Nats.connect(options);
            log.info("Successfully connected to NATS server");

            // Test the connection with a ping
            connection.flush(Duration.ofSeconds(5));
            log.info("NATS connection test successful - ping completed");

            return connection;
        } catch (Exception e) {
            log.error("Failed to connect to NATS server: {}", e.getMessage());
            log.error("Please check your NATS server configuration and network connectivity");
            throw e;
        }
    }

    @Bean
    public JetStream jetStream(Connection connection) throws IOException, InterruptedException {
        log.info("Creating JetStream instance");

        JetStream jetStream = connection.jetStream();

        log.info("JetStream instance created successfully");
        return jetStream;
    }
}
