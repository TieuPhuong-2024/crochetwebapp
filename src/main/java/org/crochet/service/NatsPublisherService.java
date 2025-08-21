package org.crochet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.crochet.payload.request.NotificationRequest;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NatsPublisherService {
    

    private final Connection natsConnection;
    private final ObjectMapper objectMapper;

    public void publishNotificationEvent(String subject, NotificationRequest notificationRequest) {
        if (natsConnection == null || !natsConnection.getStatus().equals(io.nats.client.Connection.Status.CONNECTED)) {
            log.warn("NATS connection is not available. Status: {}", natsConnection != null ? natsConnection.getStatus() : "NULL");
            return;
        }

        try {
            String jsonData = objectMapper.writeValueAsString(notificationRequest);
            natsConnection.publish(subject, jsonData.getBytes());
            log.info("Notification request published to NATS subject: {} for receiver: {}", subject, notificationRequest.getReceiverId());
        } catch (Exception e) {
            log.error("Failed to publish notification request to NATS", e);
            throw new RuntimeException("Failed to publish notification request", e);
        }
    }
}
