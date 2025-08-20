package org.crochet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.crochet.event.NatsNotificationEvent;
import org.crochet.payload.request.NotificationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class NatsConsumerService {

    private final Connection natsConnection;
    private final JetStream jetStream;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Value("${nats.jetstream.stream-name}")
    private String streamName;

    @Value("${nats.jetstream.consumer-name}")
    private String consumerName;

    @PostConstruct
    public void init() {
        log.info("Initializing NATS consumer for stream: {} with consumer: {}", streamName, consumerName);

        if (natsConnection == null || !natsConnection.getStatus().equals(io.nats.client.Connection.Status.CONNECTED)) {
            log.warn("NATS connection is not available. Consumer will not start. Status: {}",
                    natsConnection != null ? natsConnection.getStatus() : "NULL");
            return;
        }

        try {
            // Subscribe to notifications subject
            io.nats.client.Subscription subscription = natsConnection.subscribe("notifications.comment");

            // Start a background thread to handle messages
            new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        if (natsConnection.getStatus().equals(io.nats.client.Connection.Status.CONNECTED)) {
                            io.nats.client.Message msg = subscription.nextMessage(java.time.Duration.ofSeconds(10));
                            if (msg != null) {
                                String jsonData = new String(msg.getData(), StandardCharsets.UTF_8);
                                log.info("Received notification event: {}", jsonData);

                                NatsNotificationEvent event = objectMapper.readValue(jsonData, NatsNotificationEvent.class);

                                // Process the notification
                                processNotification(event);

                                log.info("Notification processed successfully for event: {}", event.getEventId());
                            }
                        } else {
                            log.warn("NATS connection is not connected. Waiting...");
                            Thread.sleep(5000); // Wait 5 seconds before retrying
                        }
                    } catch (Exception e) {
                        log.error("Failed to process notification event", e);
                        try {
                            Thread.sleep(1000); // Wait 1 second before retrying
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }).start();

            log.info("NATS consumer initialized successfully");

        } catch (Exception e) {
            log.error("Failed to initialize NATS consumer", e);
            log.error("NATS connection status: {}", natsConnection.getStatus());
            // Don't throw exception, just log the error
        }
    }

    private void processNotification(NatsNotificationEvent event) {
        try {
            NotificationRequest request = NotificationRequest.builder()
                    .title(event.getTitle())
                    .message(event.getMessage())
                    .link(event.getLink())
                    .receiverId(event.getReceiverId())
                    .senderId(event.getSenderId())
                    .notificationType(event.getNotificationType())
                    .build();

            notificationService.createNotification(request);
            log.info("Notification created successfully for receiver: {}", event.getReceiverId());

        } catch (Exception e) {
            log.error("Failed to create notification for event: {}", event.getEventId(), e);
            throw e;
        }
    }


}
