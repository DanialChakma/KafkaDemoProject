package com.oms.inventory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.inventory.entity.Inventory;
import com.oms.inventory.entity.InventoryReservation;
import com.oms.inventory.entity.OutboxEvent;
import com.oms.inventory.events.InventoryReservationResultEvent;
import com.oms.inventory.events.InventoryReserveRequestEvent;
import com.oms.inventory.events.ItemStatusDto;
import com.oms.inventory.events.OrderItemDTO;
import com.oms.inventory.repository.InventoryRepository;
import com.oms.inventory.repository.InventoryReservationRepository;
import com.oms.inventory.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    private final InventoryReservationRepository reservationRepository;
    private final ObjectMapper mapper;
    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${topics.inventory-reserved:inventory.reserved}")
    private String INVENTORY_RESERVED_TOPIC;

    @Value("${topics.inventory-failed:inventory.failed}")
    private String INVENTORY_FAILED_TOPIC;

    // inventory-reservation-failed: inventory.reservation.failed
    @Value("${topics.inventory-reservation-failed:inventory.reservation.failed}")
    private String INVENTORY_RESERVED_FAILED_TOPIC;

    @Transactional
    public Inventory updateStock(Long productId, Integer newQuantity) {
        Inventory inv = inventoryRepository.findByProductId(productId)
                .orElse(Inventory.builder()
                        .productId(productId)
                        .stockQuantity(0)
                        .build());

        inv.setStockQuantity(newQuantity);
        return inventoryRepository.save(inv);
    }

    public Optional<Inventory> getByProductId(Long productId) {
        return inventoryRepository.findByProductId(productId);
    }

    public boolean isInStock(Long productId, Integer requiredQty) {
        return inventoryRepository.findByProductId(productId)
                .map(i -> i.getStockQuantity() >= requiredQty)
                .orElse(false);
    }

    @Transactional
    public void reserveStock(Long productId, Integer qty) {
        inventoryRepository.findByProductId(productId).ifPresent(i -> {
            if (i.getStockQuantity() >= qty) {
                i.setStockQuantity(i.getStockQuantity() - qty);
                i.setReservedQuantity(i.getReservedQuantity() + qty);
                inventoryRepository.save(i);
            } else {
                throw new IllegalStateException("Insufficient stock for product " + productId);
            }
        });
    }

    @Transactional
    public void reserveInventory(String orderId, Long productId, int quantity) {
        Inventory item = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Product ID not found: " + productId));

        if (item.getStockQuantity() < quantity) {
            throw new RuntimeException("Insufficient inventory for Product: " + productId);
        }

        // 1️⃣ Decrement stock
        item.setStockQuantity(item.getStockQuantity() - quantity);
        inventoryRepository.save(item);

        // 2️⃣ Record reservation entry
        InventoryReservation reserved = InventoryReservation.builder()
                .orderId(orderId)
                .productId(productId)
                .reservedQuantity(quantity)
                .status("RESERVED")
                .build();
        reservationRepository.save(reserved);

        // 3️⃣ Record outbox event for asynchronous dispatch
        String payload = """
        {
          "orderId": "%s",
          "productId": "%d",
          "quantity": %d,
          "status": "RESERVED"
        }
        """.formatted(orderId, productId, quantity);

        outboxRepository.save(new OutboxEvent(orderId, "Inventory", "inventory.reserved", payload));

        log.info("Reserved {} units of product {} for order {}", quantity, productId, orderId);
    }

    @Transactional
    public void reserveInventoryBulk(InventoryReserveRequestEvent event) throws JsonProcessingException {
        List<ItemStatusDto> itemStatuses = new ArrayList<>();
        List<InventoryReservation> reservedItems = new ArrayList<>();
        String orderId = event.getOrderId();
        try {

            for (OrderItemDTO req : event.getItems()) {
                Inventory item = inventoryRepository.findByProductId(req.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found: " + req.getProductId()));

                if (item.getStockQuantity() < req.getQuantity()) {
                    throw new RuntimeException("Insufficient stock for product: " + req.getProductId());
                }

                // ✅ Decrement stock
                item.setStockQuantity(item.getStockQuantity() - req.getQuantity());
                inventoryRepository.save(item);

                reservedItems.add(
                        InventoryReservation.builder()
                                .orderId(orderId)
                                .productId(req.getProductId())
                                .reservedQuantity(req.getQuantity())
                                .status("RESERVED")
                                .build()
                );

                ItemStatusDto statusDto = ItemStatusDto.builder()
                        .productId(req.getProductId())
                        .sku(req.getSku())
                        .quantity(req.getQuantity())
                        .unitPrice(req.getUnitPrice())
                        .result("SUCCESS")
                        .build();
                itemStatuses.add(statusDto);
            }

            // ✅ Batch save all reservations
            reservationRepository.saveAll(reservedItems);

            // ✅ Build success event
            InventoryReservationResultEvent resultEvent = InventoryReservationResultEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .customerId(event.getCustomerId())
                    .status("RESERVED")
                    .items(itemStatuses)
                    .build();

            // ✅ Persist single outbox event

            this.publishEvent(resultEvent, INVENTORY_RESERVED_TOPIC, orderId, "inventory-service");

            log.info("✅ All items reserved for order {}", orderId);

        } catch (Exception ex) {

            log.error("❌ Inventory reservation failed for order {}: {}", orderId, ex.getMessage());
            // Rollback happens automatically due to @Transactional
            // Create partial item statuses for debug
            itemStatuses.clear();
            for (OrderItemDTO req : event.getItems()) {
                boolean reserved = reservedItems.stream()
                        .anyMatch(r -> r.getProductId().equals(req.getProductId()));

                ItemStatusDto statusDto = ItemStatusDto.builder()
                        .productId(req.getProductId())
                        .quantity(req.getQuantity())
                        .unitPrice(req.getUnitPrice())
                        .result(reserved ? "SUCCESS" : "FAILED" )
                        .build();
                itemStatuses.add(statusDto);
            }

            InventoryReservationResultEvent failedEvent = InventoryReservationResultEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .status("FAILED")
                    .reason(ex.getMessage())
                    .items(itemStatuses)
                    .build();

            // ✅ Persist failure outbox event
            outboxRepository.save(new OutboxEvent(
                    orderId,
                    "Inventory",
                    INVENTORY_RESERVED_FAILED_TOPIC,
                    mapper.writeValueAsString(failedEvent)
            ));

            // Let transaction rollback for DB integrity
            throw new RuntimeException("Inventory reservation failed: " + ex.getMessage(), ex);
        }
    }


    @Transactional
    public void releaseReservedStock(String orderId) {
        // 1️⃣ Find all reservations for this order
        List<InventoryReservation> reservations = reservationRepository.findByOrderId(orderId);

        if (reservations.isEmpty()) {
            log.warn("No inventory reservations found for order {}", orderId);
            return;
        }

        // 2️⃣ For each reserved item, add quantity back to inventory
        for (InventoryReservation reservation : reservations) {
            Inventory item = inventoryRepository.findByProductId(reservation.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + reservation.getProductId()));

            item.setStockQuantity(item.getStockQuantity() + reservation.getReservedQuantity());
            inventoryRepository.save(item);
        }

        // 3️⃣ Delete reservation entries
        reservationRepository.deleteAll(reservations);

        // 4️⃣ Record outbox event for "inventory.released"
        String payload = """
        {
          "orderId": "%s",
          "releasedItems": %s
        }
        """.formatted(
                orderId,
                reservations.stream()
                        .map(r -> String.format("{\"productId\":%d, \"quantity\":%d}", r.getProductId(), r.getReservedQuantity()))
                        .collect(Collectors.joining(",", "[", "]"))
        );

        outboxRepository.save(new OutboxEvent(orderId, "Inventory", "inventory.released", payload));

        log.info("✅ Released reserved stock for order {}", orderId);
    }


    @Transactional
    public void publishEvent(Object event, String eventType, String aggregateId, String aggregateType) {
        try {
            String payload = mapper.writeValueAsString(event);

            // 1️⃣ Persist Outbox first
            OutboxEvent outbox = OutboxEvent.builder()
                    .aggregateId(aggregateId)
                    .aggregateType(aggregateType)
                    .eventType(eventType) // eventType is the topic used for kafka
                    .published(false)
                    .payload(payload)
                    .createdAt(Instant.now())
                    .build();
            outboxRepository.save(outbox);

            // 2️⃣ Attempt to send immediately
            kafkaTemplate.send(eventType, aggregateId, payload)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Kafka send success for {} [{}]", eventType, aggregateId);
                            markOutboxAsSent(outbox.getId());
                        } else {
                            log.error("Kafka send failed for {} [{}]: {}", eventType, aggregateId, ex.getMessage());
                        }
                    });

        } catch (Exception e) {
            log.error("Error publishing event to Kafka/outbox: {}", e.getMessage());
        }
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

