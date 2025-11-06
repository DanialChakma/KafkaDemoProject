package com.oms.inventory.consumer;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.oms.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "${topics.order-created:orders.created}",
            groupId = "${spring.kafka.consumer.group-id:inventory-service}"
    )
    public void onOrderCreated(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);
            String orderId = json.get("orderId").asText();
            JsonNode items = json.get("items");

            for (JsonNode item : items) {
//                String sku = item.get("sku").asText();
                Long productId = item.get("productId").asLong();
                int quantity = item.get("quantity").asInt();
                inventoryService.reserveInventory(orderId, productId, quantity);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

