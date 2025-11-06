package com.oms.inventory.relay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.inventory.entity.OutboxEvent;
import com.oms.inventory.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper mapper;

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxRepository.findByPublishedFalse();

        for (OutboxEvent e : pendingEvents) {
            try {
                kafkaTemplate.send(e.getEventType(),
                        e.getAggregateId().toString(),
                        mapper.readTree(e.getPayload()));
                e.setPublished(true);
                outboxRepository.save(e);
                log.info("✅ Relayed Outbox event: {} for aggregate {}", e.getEventType(), e.getAggregateId());
            } catch (Exception ex) {
                log.error("💥 Failed to publish outbox event {}: {}", e.getId(), ex.getMessage());
                e.setPublished(false);
                outboxRepository.save(e);
            }
        }

    }
}

