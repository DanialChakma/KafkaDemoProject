package com.oms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.entity.Order;
import com.oms.entity.OrderItem;
import com.oms.entity.OrderStatus;
import com.oms.entity.OutboxEvent;
import com.oms.events.*;
import com.oms.repository.OrderRepository;
import com.oms.repository.OutboxRepository;
import com.oms.utils.ORDGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${topics.order-created:orders.created}")
    private String ORDER_CREATED_TOPIC;

    @Value("${topics.order-validation-request:order.validation.request}")
    private String ORDER_VALIDATION_REQUEST_TOPIC;


    // -----------------------
    // Create order (async processing)
    // -----------------------
    @Transactional
    public Order createOrderAsync(Order order) {
        Order saved;

        try {

            order.setStatus(OrderStatus.PENDING);
            order.setOrderNumber(ORDGenerator.genOrdNumber());

            BigDecimal totalAmount = order.getItems().stream()
                    .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            order.setTotalAmount(totalAmount); // it should come from item list calculation

            saved = orderRepository.save(order);

            // Prepare validation request
            var itemList = saved.getItems().stream().map(o->{
                OrderItemDto dto = new OrderItemDto();
                dto.setProductId(o.getProductId());
                dto.setQuantity(o.getQuantity());
                dto.setSku(o.getSku());
                dto.setUnitPrice(o.getUnitPrice());
                return dto;
            }).collect(Collectors.toList());


            OrderValidationRequestEvent event = OrderValidationRequestEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(saved.getId())
                    .customerId(saved.getCustomerId())
                    .items(itemList)
                    .build();


            this.publishEvent(event, ORDER_VALIDATION_REQUEST_TOPIC, saved.getId(), "order-service");

            log.info("Published OrderValidationRequestEvent for orderId={}", saved.getId());

        } catch (Exception e) {
            log.error("❌ Failed to serialize OrderCreatedEvent: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to persist outbox event", e);
        }

        return saved;
    }


    /**
     * Update order status (e.g. CONFIRMED, CANCELLED, PAYMENT_FAILED, etc.)
     * @param orderId UUID of the order
     * @param status  new status (string or enum name)
     * @param reason  optional reason (e.g. cancellation cause)
     */
    @Transactional
    public void updateStatus(String orderId, String status, String reason) {
        Optional<Order> optionalOrder = orderRepository.findById(orderId);

        if (optionalOrder.isEmpty()) {
            log.warn("⚠️ Order with ID {} not found. Cannot update status.", orderId);
            return;
        }

        Order order = optionalOrder.get();

        try {
            OrderStatus newStatus = OrderStatus.valueOf(status.toUpperCase().trim());
            OrderStatus currentStatus = order.getStatus();

            // Avoid unnecessary updates or regressions
            if (currentStatus == newStatus) {
                log.info("ℹ️ Order {} already in status {} — no update needed.", orderId, currentStatus);
                return;
            }

            // Basic validation rules (optional)
            if (currentStatus == OrderStatus.CANCELLED || currentStatus == OrderStatus.DELIVERED) {
                log.warn("❌ Cannot update order {}. It is already in terminal state: {}", orderId, currentStatus);
                return;
            }

            order.setStatus(newStatus);

            // Optionally store the reason if you have such a field
            if (reason != null && !reason.isBlank()) {
                try {
                    // only if your entity has such a column
                    order.getClass().getMethod("setReason", String.class).invoke(order, reason);
                } catch (Exception ignore) {
                    log.debug("No 'reason' field available in Order entity; skipping reason assignment.");
                }
            }

            orderRepository.save(order);
            log.info("✅ Order {} status updated: {} → {}", orderId, currentStatus, newStatus);

        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid status '{}' provided for order {}", status, orderId);
        } catch (Exception e) {
            log.error("💥 Failed to update order {} status to {}: {}", orderId, status, e.getMessage(), e);
        }
    }

    private OrderItemDto toDto(OrderItem it) {
        return new OrderItemDto(it.getProductId(), it.getSku(), it.getQuantity(), it.getUnitPrice());
    }

    @Transactional
    public void handleOrderCancelledEvent(OrderCancelledEvent event) {
        try {
            log.info("Received OrderCancelledEvent for orderId={} reason={}", event.getOrderId(), event.getReason());
            String reason = event.getReason();
            reason = reason != null && !reason.isEmpty() ? reason : "N/A";
            Order order = orderRepository.findById(event.getOrderId())
                    .orElse(null);

            if (order == null) {
                log.warn("Order not found for ID: {}", event.getOrderId());
                return;
            }

            order.setStatus(OrderStatus.CANCELLED);
            order.setReason(reason);
            orderRepository.save(order);

            log.info("Order {} marked as CANCELLED in Order Service", event.getOrderId());
        }catch (Exception ex){

        }

    }

    @Transactional
    public void handleOrderStateChangeEvent(OrderStatusUpdatedEvent event) {

        log.info("Received OrderStateChangeEvent for orderId={}", event.getOrderId());
        orderRepository.findById(event.getOrderId()).ifPresent(order -> {
            order.setStatus( OrderStatus.valueOf(event.getStatus().trim()) );
            order.setReason(event.getReason());
            orderRepository.save(order);
            log.info("📦 Order {} updated to {} (reason: {})",
                    event.getOrderId(), event.getStatus(), event.getReason());
        });

    }



    @Transactional
    public void publishEvent(Object event, String eventType, String aggregateId, String aggregateType) {
        try {
            String payload = objectMapper.writeValueAsString(event);

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


