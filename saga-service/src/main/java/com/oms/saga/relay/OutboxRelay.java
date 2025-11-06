package com.oms.saga.relay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.saga.entity.OutboxEvent;
import com.oms.saga.repository.OutboxRepository;
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

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper mapper;

    @Transactional
    @Scheduled(fixedDelay = 3000) // every 3s
    public void relayPendingEvents() {

        List<OutboxEvent> pendingEvents = outboxRepository.findByPublishedFalse();

        if (pendingEvents.isEmpty()) {
            log.debug("No pending outbox events to publish.");
            return;
        }

        log.info("Found {} pending outbox events to publish", pendingEvents.size());

        for (OutboxEvent e : pendingEvents) {
            try {
                kafkaTemplate.send(
                            e.getEventType(),
                            e.getAggregateId(),
                            mapper.readTree(e.getPayload())
                        )
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                e.setPublished(true);
                                e.setUpdatedAt(Instant.now());
                                outboxRepository.save(e);
                                log.info("Resent outbox event {} -> {}", e.getEventType(), e.getAggregateId());
                            } else {
                                log.warn("Failed resend of {}: {}", e.getEventType(), ex.getMessage());
                            }
                        });

                log.info("Relayed Outbox event: {} for aggregate {}", e.getEventType(), e.getAggregateId());
            } catch (Exception ex) {
                log.error("Failed to publish outbox event {}: {}", e.getId(), ex.getMessage());
            }
        }
    }
}

