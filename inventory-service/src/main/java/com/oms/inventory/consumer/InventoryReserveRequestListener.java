package com.oms.inventory.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.inventory.events.InventoryReserveRequestEvent;
import com.oms.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryReserveRequestListener {

    private final ObjectMapper mapper;
    private final InventoryService inventoryService;


    @KafkaListener(
    topics = "${topics.inventory-reserve-request:inventory.reserve.request}",
    groupId = "inventory-service")
    public void onReserveRequest(String message) throws Exception {

        log.info("📦 Received inventory reserve request: {}", message);
        InventoryReserveRequestEvent event = mapper.readValue(message, InventoryReserveRequestEvent.class);
        /*
        JsonNode node = mapper.readTree(message);
        String orderId = node.get("orderId").asText();
        List<InventoryItemRequest> items = new ArrayList<>();
        for (JsonNode itemNode : node.get("items")) {
            Long productId = itemNode.get("productId").asLong();
            int quantity = itemNode.get("quantity").asInt();
            items.add(new InventoryItemRequest(productId, quantity));
        }
        */

        inventoryService.reserveInventoryBulk(event);

    }


}

