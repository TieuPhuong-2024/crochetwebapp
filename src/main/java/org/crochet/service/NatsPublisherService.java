package org.crochet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class NatsPublisherService {

    private final Connection natsConnection;
    private final ObjectMapper objectMapper;

    public void publishNotificationEvent(String subject, Object eventData) {
        if (natsConnection == null || !natsConnection.getStatus().equals(io.nats.client.Connection.Status.CONNECTED)) {
            log.warn("NATS connection is not available. Status: {}", natsConnection != null ? natsConnection.getStatus() : "NULL");
            return;
        }

        try {
            String jsonData = objectMapper.writeValueAsString(eventData);
            byte[] payload = jsonData.getBytes(StandardCharsets.UTF_8);

            log.info("Publishing notification event to subject: {} with data: {}", subject, jsonData);

            natsConnection.publish(subject, payload);
            log.info("Event published successfully to subject: {}", subject);

        } catch (Exception e) {
            log.error("Failed to publish notification event to subject: {}", subject, e);
            log.error("NATS connection status: {}", natsConnection.getStatus());
            // Don't throw exception, just log the error to prevent app crash
        }
    }
}
