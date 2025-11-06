package com.oms.saga.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.saga.entity.OutboxEvent;
import com.oms.saga.entity.Saga;
import com.oms.saga.entity.SagaMessageLog;
import com.oms.saga.events.*;
import com.oms.saga.repository.OutboxRepository;
import com.oms.saga.repository.SagaMessageLogRepository;
import com.oms.saga.repository.SagaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SagaOrchestratorService {

    private final OutboxRepository outboxRepository;
    private final SagaRepository sagaRepository;
    private final SagaMessageLogRepository logRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper mapper;


    @Value("${topics.order-cancelled:orders.cancelled}")
    private String ORDER_CANCELLED_TOPIC;

    @Value("${topics.order-status-updated:orders.status.updated}")
    private String ORDER_UPDATED_TOPIC;

    @Value("${topics.inventory-reserve-request:inventory.reserve.request}")
    private String INVENTORY_RESERVE_REQUEST_TOPIC;

    @Value("${topics.inventory-release:inventory.release}")
    private String INVENTORY_RELEASE_TOPIC;

    @Value("${topics.payments-request:payment.request}")
    private String PAYMENT_REQUEST_TOPIC;

    @Value("${topics.payments-refund:payments.refund}")
    private String PAYMENT_REFUND_TOPIC;


    private void logSagaEvent(String eventId, String orderId, String eventType, Object event,
                              String sourceService, String status, Exception error) {
        try {
            // avoid duplicate log entries
            if (eventId != null && logRepository.findByEventId(eventId).isPresent()) {
                log.info("Duplicate event ignored for ID: {}", eventId);
                return;
            }

            String payload = mapper.writeValueAsString(event);

            SagaMessageLog logEntry = SagaMessageLog.builder()
                    .eventId(eventId != null ? eventId : UUID.randomUUID().toString())
                    .sagaId(orderId != null ? orderId : "UNKNOWN")
                    .eventType(eventType)
                    .payload(payload)
                    .sourceService(sourceService)
                    .status(status != null ? status : "PROCESSED")
                    .receivedAt(Instant.now())
                    .build();

            if (error != null) {
                logEntry.setStatus("FAILED: " + error.getMessage());
            }

            logRepository.save(logEntry);

            log.info("Saga event logged: [{}] {}", eventType, eventId);
        } catch (Exception ex) {
            log.error("Failed to log saga event [{}]: {}", eventType, ex.getMessage(), ex);
        }
    }


    @Transactional
    public void startInventoryReservation(InventoryReserveRequestEvent event) throws JsonProcessingException {
        try {

            log.info("InventoryReserveRequestEvent: {}", event);
            // ✅ log receipt
            logSagaEvent(event.getEventId(), event.getOrderId(),
                    "InventoryReserveRequestEvent",
                    event, "saga-service",
                    "RECEIVED", null);

            Saga saga = getOrCreateSaga(event.getOrderId());

            if (isDuplicate(saga, event.getEventId())) return;

            saga.setInventoryReserved(true);
            saga.setLastUpdated(Instant.now());
            saga.markProcessed(event.getEventId());
            sagaRepository.save(saga);

            String payload = mapper.writeValueAsString(event);
            kafkaTemplate.send(INVENTORY_RESERVE_REQUEST_TOPIC, event.getOrderId(), payload);

            // ✅ final success log
            logSagaEvent(event.getEventId(), event.getOrderId(),
                    "InventoryReserveRequestEvent", event, "saga-service", "PROCESSED", null);

//            log.info("Sent inventory.reserve.request for order {} with {} items", orderId, event.getItems().size());

        } catch (Exception ex) {
//            log.error("Failed to send inventory.reserve.request for order {}: {}", event.getOrderId(), ex.getMessage());
            logSagaEvent(event.getEventId(), event.getOrderId(),
                    "InventoryReserveRequestEvent", event, "saga-service", "FAILED", ex);
            throw ex;
        }
    }

    // ---------------------
    //  ORDER FAILED DUE TO VALIDATION
    // ---------------------
    public void cancelOrderDueToInvalidData(OrderCancelledEvent event) throws JsonProcessingException {

        // publish order.cancelled event (compensation)
        try{
            log.info("handleOrderCancelledEvent: {}", event);
            // ✅ log receipt
            logSagaEvent(event.getEventId(), event.getOrderId(),
                    "OrderCancelledEvent",
                    event, "saga-service",
                    "RECEIVED", null);

            Saga saga = getOrCreateSaga(event.getOrderId());

            if (isDuplicate(saga, event.getEventId())) return;

            saga.setInventoryReserved(true);
            saga.setLastUpdated(Instant.now());
            saga.markProcessed(event.getEventId());
            sagaRepository.save(saga);

            String payload = mapper.writeValueAsString(event);
            kafkaTemplate.send(ORDER_CANCELLED_TOPIC, String.valueOf(event.getOrderId()), payload);

            // ✅ final success log
            logSagaEvent(event.getEventId(), event.getOrderId(),
                    "OrderCancelledEvent", event, "saga-service", "PROCESSED", null);

        } catch (Exception ex) {
            logSagaEvent(event.getEventId(), event.getOrderId(),
                    "OrderCancelledEvent", event, "saga-service", "FAILED", ex);
            throw ex;
        }


    }


    // ---------------------
    //  INVENTORY RESERVED
    // ---------------------
    @Transactional
    public void handleInventoryReserved(InventoryReservedEvent event) {

        try{
            log.info("handleInventoryReserved: {}", event);
            // ✅ log receipt
            logSagaEvent(event.getEventId(), event.getOrderId(),
                    "InventoryReservedEvent",
                    event, "inventory-service",
                    "RECEIVED", null);

            Saga saga = getOrCreateSaga(event.getOrderId());

            if (isDuplicate(saga, event.getEventId())) return;

            saga.setInventoryReserved(true);
            saga.setLastUpdated(Instant.now());
            saga.markProcessed(event.getEventId());
            sagaRepository.save(saga);

            // ✅ STEP 1: Trigger Payment after inventory reservation
            triggerPaymentProcess(event, saga);

            // ✅ If payment already succeeded (edge case), finalize success
            if (saga.isPaymentSucceeded()) {
                finalizeSuccess(event.getOrderId(), saga);
            }

            // ✅ final success log
            logSagaEvent(event.getEventId(), event.getOrderId(),
                    "InventoryReservedEvent", event, "inventory-service", "PROCESSED", null);

        } catch (Exception ex) {
            logSagaEvent(event.getEventId(), event.getOrderId(),
                    "InventoryReservedEvent", event, "inventory-service", "FAILED", ex);
            throw ex;
        }
    }

    private void triggerPaymentProcess(InventoryReservedEvent inventoryEvent, Saga saga) {
        try {

            BigDecimal totalAmount = inventoryEvent.getItems()
                    .stream()
                    .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Construct the payment initiation event
            PaymentProcessEvent paymentEvent = PaymentProcessEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(inventoryEvent.getOrderId())
                    .customerId(inventoryEvent.getCustomerId())
                    .totalAmount(totalAmount)
                    .paymentMethod("CARD") // or dynamic from order
                    .createdAt(Instant.now())
                    .build();

            this.publishEvent(paymentEvent, PAYMENT_REQUEST_TOPIC, inventoryEvent.getOrderId(), "saga-service");

            log.info("Triggered payment.request for order {} (amount: {})", inventoryEvent.getOrderId(), totalAmount);

            // Log saga event
            logSagaEvent(paymentEvent.getEventId(),
                    inventoryEvent.getOrderId(),
                    "PaymentProcessEvent",
                    paymentEvent,
                    "saga-service",
                    "PUBLISHED",
                    null);

        } catch (Exception e) {
            // 🔄 In case Kafka send fails, optionally persist to Outbox
            log.error("Failed to publish payment.process for {}: {}", inventoryEvent.getOrderId(), e.getMessage());

            logSagaEvent(null,
                    inventoryEvent.getOrderId(),
                    "PaymentProcessEvent",
                    inventoryEvent,
                    "saga-service",
                    "FAILED_TO_PUBLISH",
                    e);

            // Optional fallback: persist to Outbox for retry
            saveOutboxFallback(PAYMENT_REQUEST_TOPIC, inventoryEvent.getOrderId(), inventoryEvent);
        }
    }


    private void saveOutboxFallback(String topic, String key, Object payload) {
        try {
            SagaMessageLog logEntry = SagaMessageLog.builder()
                    .eventId(UUID.randomUUID().toString())
                    .sagaId(key)
                    .eventType("OUTBOX_RETRY:" + topic)
                    .payload(mapper.writeValueAsString(payload))
                    .sourceService("saga-service")
                    .status("PENDING_RETRY")
                    .receivedAt(Instant.now())
                    .build();
            logRepository.save(logEntry);
            log.info("💾 Saved message to outbox for topic {}", topic);
        } catch (Exception ex) {
            log.error("❌ Failed to save message to outbox: {}", ex.getMessage());
        }
    }



    // ---------------------
    //  INVENTORY FAILED
    // ---------------------
    @Transactional
    public void handleInventoryFailed(InventoryReservationFailedEvent event) {

        try {
            log.info("handleInventoryFailed: {}", event);
            // ✅ log receipt
            logSagaEvent(event.getEventId(), event.getOrderId(),
                    "InventoryFailedEvent",
                    event, "inventory-service",
                    "RECEIVED", null);

            Saga saga = getOrCreateSaga(event.getOrderId());

            if (isDuplicate(saga, event.getEventId())) return;

            saga.setStatus("FAILED");
            saga.setLastUpdated(Instant.now());
            saga.markProcessed(event.getEventId());
            sagaRepository.save(saga);

            // ✅ Compensation trigger
            if (saga.isPaymentSucceeded()) {
                publishPaymentRefund(event.getOrderId(), "COMPENSATION: INVENTORY_FAILED");
            }

            notifyOrderServiceFailed(event.getOrderId(), "INVENTORY_FAILED: " + event.getReason());

            // ✅ log receipt
            logSagaEvent(event.getEventId(), event.getOrderId(),
                    "InventoryFailedEvent",
                    event, "inventory-service",
                    "PROCESSED", null);

        }catch (Exception ex){
            // ✅ log receipt
            logSagaEvent(event.getEventId(), event.getOrderId(),
                    "InventoryFailedEvent",
                    event, "inventory-service",
                    "FAILED", ex);
            throw ex;
        }
    }


    // ---------------------
    //  INVENTORY RELEASED
    // ---------------------
    @Transactional
    public void handleInventoryReleased(InventoryReleasedEvent event) {

        try {
            log.info("handleInventoryReleased: {}", event);
            // ✅ log receipt
            logSagaEvent(event.getEventId(), event.getOrderId(),
                    "InventoryReleasedEvent",
                    event, "inventory-service",
                    "RECEIVED", null);

            Saga saga = sagaRepository.findById(event.getOrderId()).orElse(null);
            if (saga == null) return;

            if (isDuplicate(saga, event.getEventId())) return;

            saga.setInventoryReserved(false);
            saga.setLastUpdated(Instant.now());
            saga.markProcessed(event.getEventId());
            sagaRepository.save(saga);

            if ("FAILED".equalsIgnoreCase(saga.getStatus()) && saga.isPaymentSucceeded()) {
                publishPaymentRefund(event.getOrderId(), "COMPENSATION: INVENTORY_RELEASED_AFTER_FAILURE");
            }


            if ("FAILED".equalsIgnoreCase(saga.getStatus())) {
                notifyOrderServiceFailed(event.getOrderId(), "INVENTORY_RELEASED: " + event.getReason());
            }

            logSagaEvent(event.getEventId(), event.getOrderId(),
                    "InventoryReleasedEvent",
                    event, "inventory-service",
                    "PROCESSED", null);
        }catch (Exception ex){
            logSagaEvent(event.getEventId(), event.getOrderId(),
                    "InventoryReleasedEvent",
                    event, "inventory-service",
                    "FAILED", ex);
            throw ex;
        }

    }


    // ---------------------
    //  PAYMENT SUCCESS
    // ---------------------
    @Transactional
    public void handlePaymentSuccess(PaymentSuccessEvent event) {

        try {

            log.info("handlePaymentSuccess: {}", event);
            // ✅ log receipt
            logSagaEvent(event.getEventId(), event.getOrderId(),
                    "PaymentSuccessEvent",
                    event, "payment-service",
                    "RECEIVED", null);

            Saga saga = getOrCreateSaga(event.getOrderId());

            if (isDuplicate(saga, event.getEventId())) return;

            saga.setPaymentSucceeded(true);
            saga.setLastUpdated(Instant.now());
            saga.markProcessed(event.getEventId());
            sagaRepository.save(saga);

            if (saga.isInventoryReserved()) {
                finalizeSuccess(event.getOrderId(), saga);
            }

            logSagaEvent(event.getEventId(), event.getOrderId(),
                    "PaymentSuccessEvent",
                    event, "payment-service",
                    "PROCESSED", null);
        }catch (Exception ex){
            logSagaEvent(event.getEventId(), event.getOrderId(),
                    "PaymentSuccessEvent",
                    event, "payment-service",
                    "FAILED", ex);
            throw ex;
        }

    }


    // ---------------------
    //  PAYMENT FAILED
    // ---------------------
    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("handlePaymentFailed: {}", event);
        try {
            logSagaEvent(event.getEventId(), event.getOrderId(),
                    "PaymentFailedEvent", event, "payment-service", "RECEIVED", null);

            Saga saga = getOrCreateSaga(event.getOrderId());

            if (isDuplicate(saga, event.getEventId())) return;

            saga.setStatus("FAILED");
            saga.setLastUpdated(Instant.now());
            saga.markProcessed(event.getEventId());
            sagaRepository.save(saga);

            if (saga.isInventoryReserved()) {
                publishInventoryRelease(event.getOrderId());
            }

            //no refund needed here since this is the failed payment case.

            notifyOrderServiceFailed(event.getOrderId(), "PAYMENT_FAILED: " + event.getReason());

            logSagaEvent(event.getEventId(), event.getOrderId(),
                    "PaymentFailedEvent",
                    event,
                    "payment-service",
                    "PROCESSED", null);

        } catch (Exception ex){
            logSagaEvent(event.getEventId(), event.getOrderId(),
                    "PaymentFailedEvent", event, "payment-service", "FAILED", ex);
            throw ex;
        }
    }


    @Transactional
    public void handlePaymentRefunded(PaymentRefundedEvent event) {
        try {
            log.info("handlePaymentRefunded: {}", event);
            logSagaEvent(event.getEventId(), event.getOrderId(),
                    "PaymentRefundedEvent", event, "payment-service", "RECEIVED", null);

            Saga saga = sagaRepository.findById(event.getOrderId()).orElse(null);
            if (saga == null) {
                log.warn("No saga found for refunded payment of order {}", event.getOrderId());
                return;
            }

            if (isDuplicate(saga, event.getEventId())) return;

            saga.setStatus("COMPENSATED");
            saga.setLastUpdated(Instant.now());
            saga.markProcessed(event.getEventId());
            sagaRepository.save(saga);

            logSagaEvent(event.getEventId(), event.getOrderId(),
                    "PaymentRefundedEvent", event, "payment-service", "PROCESSED", null);

            log.info("✅ Saga compensation completed for order {}", event.getOrderId());
        } catch (Exception ex) {
            logSagaEvent(event.getEventId(), event.getOrderId(),
                    "PaymentRefundedEvent", event, "payment-service", "FAILED", ex);
            throw ex;
        }
    }



    // ---------------------
    //  Utility Methods
    // ---------------------
    private Saga getOrCreateSaga(String orderId) {
        return sagaRepository.findById(orderId).orElseGet(() -> {
            Saga newSaga = Saga.builder()
                    .orderId(orderId)
                    .inventoryReserved(false)
                    .paymentSucceeded(false)
                    .status("PENDING")
                    .lastUpdated(Instant.now())
                    .build();
            return sagaRepository.save(newSaga); // <-- persist
        });
    }

    private boolean isDuplicate(Saga saga, String eventId) {
        if (eventId == null || saga.hasProcessed(eventId)) {
            log.info("Duplicate or missing eventId for saga {}, skipping...", saga.getOrderId());
            return true;
        }
        return false;
    }

    private void finalizeSuccess(String orderId, Saga saga) {
        saga.setStatus("COMPLETED");
        saga.setLastUpdated(Instant.now());
        sagaRepository.save(saga);
        notifyOrderServiceCompleted(orderId);
    }


    private void notifyOrderServiceCompleted(String orderId) {
        OrderStatusUpdatedEvent event = null;
        try {
            event = OrderStatusUpdatedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .status("CONFIRMED")
                    .timestamp(Instant.now())
                    .build();

            this.publishEvent(event, ORDER_UPDATED_TOPIC, orderId, "saga-service");

            log.info("✅ Published order.status.updated CONFIRMED for order {}", orderId);

            logSagaEvent(event.getEventId(), orderId,
                    "OrderStatusUpdatedEvent", event, "saga-service", "PROCESSED", null);

        } catch (Exception e) {
            log.error("Failed to publish order.status.updated for {}: {}", orderId, e.getMessage());
            logSagaEvent(event != null ? event.getEventId(): null,
                    orderId,
                    "OrderStatusUpdatedEvent", null, "saga-service", "FAILED", e);
        }
    }



    private void notifyOrderServiceFailed(String orderId, String reason) {
        try {
            OrderStatusUpdatedEvent event = OrderStatusUpdatedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .status("CANCELLED")
                    .reason(reason)
                    .timestamp(Instant.now())
                    .build();

            this.publishEvent(event, ORDER_UPDATED_TOPIC, orderId, "saga-service");

            log.info("Published order.status.updated CANCELLED for order {} (reason: {})", orderId, reason);

            logSagaEvent(event.getEventId(), orderId,
                    "OrderStatusUpdatedEvent", event, "saga-service", "PROCESSED", null);

        } catch (Exception e) {
            log.error("Failed to publish order.status.updated CANCELLED for {}: {}", orderId, e.getMessage());
            logSagaEvent(UUID.randomUUID().toString(), orderId,
                    "OrderStatusUpdatedEvent", null, "saga-service", "FAILED", e);
        }
    }



    private void publishInventoryRelease(String orderId) {
        try {

            InventoryReleaseEvent event = InventoryReleaseEvent.builder()
                    .orderId(orderId)
                    .eventId(UUID.randomUUID().toString())
                    .build();

            this.publishEvent(event, INVENTORY_RELEASE_TOPIC, orderId, "saga-service");

            log.info("Published inventory.release for order {}", orderId);
        } catch (Exception e) {
            log.error("Failed to publish inventory.release for {}: {}", orderId, e.getMessage());
        }
    }

    private void publishPaymentRefund(String orderId, String reason) {
        try {

            PaymentRefundEvent event = PaymentRefundEvent.builder()
                    .orderId(orderId)
                    .reason(reason)
                    .eventId(UUID.randomUUID().toString())
                    .build();

            this.publishEvent(event, PAYMENT_REFUND_TOPIC, orderId, "saga-service");

            log.info("💸 Published payment.refund for order {} (reason: {})", orderId, reason);
        } catch (Exception e) {
            log.error("❌ Failed to publish payment.refund for {}: {}", orderId, e.getMessage());
        }
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
