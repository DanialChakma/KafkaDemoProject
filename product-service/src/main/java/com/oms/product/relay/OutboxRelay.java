package com.oms.product.relay;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.product.entity.OutboxEvent;
import com.oms.product.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

        List<OutboxEvent> pendingEvents = outboxRepository.findByPublishedFalseAndNextAttemptAtBefore(Instant.now());

        if (pendingEvents.isEmpty()) {
            log.debug("No pending outbox events to publish.");
            return;
        }

        log.info("🚀 Found {} pending outbox events to publish", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {

            try {

                kafkaTemplate
                        .send(
                                event.getEventType(),
                                event.getAggregateId(),
                                event.getPayload()
                        )
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                markOutboxAsSent(event.getId());
                                log.info("Resent outbox event {} -> {}", event.getEventType(), event.getAggregateId());
                            } else {
                                markOutboxAsFailed(event.getId());
                                log.warn("Failed resend of {}: {}", event.getEventType(), ex.getMessage());
                            }
                        });

                log.info("Published event to topic [{}]", event.getEventType());
            } catch (Exception e) {
                markOutboxAsFailed(event.getId());
                log.error("Failed to publish event {}: {}", event.getId(), e.getMessage());
                throw e; // triggers retry
            }
        }
    }


    @Transactional
    protected void markOutboxAsFailed(Long outboxId) {
        outboxRepository.findById(outboxId).ifPresent(e -> {
            e.setRetryCount(e.getRetryCount() + 1);
            e.setLastAttemptAt(Instant.now());
            // exponential backoff: e.g., 2^retryCount seconds
            long delaySeconds = (long) Math.pow(2, e.getRetryCount());
            e.setNextAttemptAt(Instant.now().plusSeconds(delaySeconds));
            outboxRepository.save(e);
        });
    }

    @Transactional
    protected void markOutboxAsSent(Long outboxId) {
        outboxRepository.findById(outboxId).ifPresent(o -> {
            o.setPublished(true);
            o.setUpdatedAt(Instant.now());
            outboxRepository.save(o);
        });
    }


    /**
     * Fallback method for when retry attempts are exhausted
     */
    private void fallbackPublish(Exception ex) {
        log.error("🔥 Retry exhausted: {}", ex.getMessage());
    }


}

