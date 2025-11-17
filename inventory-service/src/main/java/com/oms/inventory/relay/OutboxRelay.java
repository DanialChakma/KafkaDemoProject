package com.oms.inventory.relay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.inventory.entity.OutboxEvent;
import com.oms.inventory.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper mapper;

    @Scheduled(fixedDelay = 5000) // 5s
    public void publishPendingEvents() {

        Instant now = Instant.now();
        List<OutboxEvent> pendingEvents = outboxRepository.findByPublishedFalseAndNextAttemptAtBefore(now);

        for (OutboxEvent e : pendingEvents) {
            try {
                kafkaTemplate.send(
                        e.getEventType(),
                        e.getAggregateId(),
                        e.getPayload()
                ).whenComplete((result, ex) -> {
                    if (ex == null) {
                        markOutboxAsSent(e.getId());
                        log.info("Resent outbox event {} -> {}", e.getEventType(), e.getAggregateId());
                    } else {
                        markOutboxAsFailed(e.getId());
                        log.warn("Failed resend of {}: {}", e.getEventType(), ex.getMessage());
                    }
                });

                log.info("✅ Relayed Outbox event: {} for aggregate {}", e.getEventType(), e.getAggregateId());
            } catch (Exception ex) {
                log.error("💥 Failed to publish outbox event {}: {}", e.getId(), ex.getMessage());
                markOutboxAsFailed(e.getId());
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

}

