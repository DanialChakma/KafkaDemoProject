package com.oms.payment.relay;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.payment.entity.OutboxEvent;
import com.oms.payment.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
//import io.github.resilience4j.retry.annotation.Retry;
//Kafka Producer (publishing payment.success events from Outbox)
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper mapper;

    @Scheduled(fixedDelay = 5000)
//    @Retry(name = "kafkaPublisher", fallbackMethod = "fallbackPublish")
    public void publishPendingEvents() throws JsonProcessingException {
        List<OutboxEvent> pendingEvents = outboxRepository.findByPublishedFalse();

        if (pendingEvents.isEmpty()) {
            log.debug("No pending outbox events to publish.");
            return;
        }

        log.info("🚀 Found {} pending outbox events to publish", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
//                String topic = resolveTopic(event.getEventType());
                kafkaTemplate
                        .send(
                                event.getEventType(),
                                event.getAggregateId(),
                                mapper.readTree(event.getPayload())
                        )
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                event.setPublished(true);
                                event.setUpdatedAt(Instant.now());
                                outboxRepository.save(event);
                                log.info("Resent outbox event {} -> {}", event.getEventType(), event.getAggregateId());
                            } else {
                                log.warn("Failed resend of {}: {}", event.getEventType(), ex.getMessage());
                            }
                        });

                log.info("Published event to topic [{}]", event.getEventType());
            } catch (Exception e) {
                log.error("Failed to publish event {}: {}", event.getId(), e.getMessage());
                throw e; // triggers retry
            }
        }
    }


    /**
     * Fallback method for when retry attempts are exhausted
     */
    private void fallbackPublish(Exception ex) {
        log.error("🔥 Retry exhausted: {}", ex.getMessage());
    }

    /**
     * Helper to map event types to Kafka topics
     */
    private String resolveTopic(String eventType) {
        return switch (eventType) {
            case "payment.success" -> "payments.success";
            case "payment.failed" -> "payments.failed";
            case "inventory.reserved" -> "inventory.reserved";
            default -> "unknown.events";
        };
    }

}

