package org.crochet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.crochet.payload.request.NotificationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class NatsConsumerService {

    private final Connection natsConnection;
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
            // Create dispatcher
            Dispatcher dispatcher = natsConnection.createDispatcher();
            
            // Subscribe to notifications subject
            dispatcher.subscribe("notifications.comment", msg -> {
                try {
                    String jsonData = new String(msg.getData(), StandardCharsets.UTF_8);
                    log.info("Received notification event: {}", jsonData);

                    NotificationRequest request = objectMapper.readValue(jsonData, NotificationRequest.class);
                    notificationService.createNotification(request);
                    
                    log.info("Notification processed successfully for receiver: {}", request.getReceiverId());
                    
                } catch (Exception e) {
                    log.error("Failed to process notification event", e);
                }
            });

            log.info("NATS consumer initialized successfully");

        } catch (Exception e) {
            log.error("Failed to initialize NATS consumer", e);
            log.error("NATS connection status: {}", natsConnection.getStatus());
        }
    }
}
