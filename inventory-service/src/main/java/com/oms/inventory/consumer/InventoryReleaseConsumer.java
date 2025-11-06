package com.oms.inventory.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryReleaseConsumer {

    private final ObjectMapper objectMapper;
    private final InventoryService inventoryService;

    @KafkaListener(topics = "${topics.inventory-release:inventory.release}",
            groupId = "${spring.kafka.consumer.group-id:inventory-service}"
    )
    public void onInventoryRelease(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);
            String orderId = json.get("orderId").asText();
            log.info("📦 Received inventory.release for order {}", orderId);
            inventoryService.releaseReservedStock(orderId);
        } catch (Exception ex) {
            log.error("Failed to process inventory.release: {}", ex.getMessage(), ex);
        }
    }
}

