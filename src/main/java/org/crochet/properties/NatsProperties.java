package org.crochet.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "nats")
public class NatsProperties {
    private String servers;
    private String connectionName;

    private JetStreamProperties jetstream;

    @Data
    public static class JetStreamProperties {
        private boolean enabled;
        private String streamName;
        private String consumerName;
    }
}
