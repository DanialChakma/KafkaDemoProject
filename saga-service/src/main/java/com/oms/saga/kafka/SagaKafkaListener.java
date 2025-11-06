package com.oms.saga.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.saga.events.*;
import com.oms.saga.service.SagaOrchestratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SagaKafkaListener {

    private final ObjectMapper mapper;
    private final SagaOrchestratorService orchestrator;

    private final ConcurrentHashMap<String, Boolean> customerValidated = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ProductValidationResultEvent> productValidationResults = new ConcurrentHashMap<>();

    /*
    * listener
    * event source: customer service
    * purpose: validate if customer exist or not in an order
    * */
    @KafkaListener(topics = "${customer-validation-result:customer.validation.result}",
            groupId = "${spring.kafka.consumer.group-id:saga-orchestrator}"
    )
    public void onCustomerValidation(String message) throws Exception {
        CustomerValidationResultEvent e = mapper.readValue(message, CustomerValidationResultEvent.class);
        customerValidated.put(e.getOrderId(), e.isValid());
        checkValidationComplete(e.getOrderId());
    }

    /*
     * listener
     * event source: product service
     * purpose: validate products in an order if all products exist or not in the product catalog
     * */
    @KafkaListener(topics = "${product-validation-result:product.validation.result}",
            groupId = "${spring.kafka.consumer.group-id:saga-orchestrator}"
    )
    public void onProductValidation(String message) throws Exception {
        ProductValidationResultEvent e = mapper.readValue(message, ProductValidationResultEvent.class);
        productValidationResults.put(e.getOrderId(), e);
        checkValidationComplete(e.getOrderId());
    }

    private void checkValidationComplete(String orderId) throws JsonProcessingException {
        Boolean c = customerValidated.get(orderId);
        ProductValidationResultEvent  p = productValidationResults.get(orderId);
        if (c != null && p != null) {
            if (c && p.isValid()) {
                log.info("All validations passed for order {}", orderId);

                InventoryReserveRequestEvent requestEvent = InventoryReserveRequestEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .customerId(p.getCustomerId())
                        .orderId(p.getOrderId())
                        .items(p.getItems())
                        .build();
                orchestrator.startInventoryReservation(requestEvent);
            } else {
                log.warn("Validation failed for order {} (customerValid={}, productValid={})", orderId, c, p);
                OrderCancelledEvent event = new OrderCancelledEvent(
                    orderId,
                    "Invalid customer or product during validation",
                    UUID.randomUUID().toString()
                );
                orchestrator.cancelOrderDueToInvalidData(event);
            }
            customerValidated.remove(orderId);
            productValidationResults.remove(orderId);
        }
    }

    /*
     * listener
     * event source: inventory service
     * purpose: confirming if inventory successfully reserved an order's items
     * */
    @KafkaListener(topics = "${topics.inventory-reserved:inventory.reserved}",
            groupId = "${spring.kafka.consumer.group-id:saga-orchestrator}"
    )
    public void onInventoryReserved(String message) throws Exception {
        InventoryReservedEvent e = mapper.readValue(message, InventoryReservedEvent.class);
        orchestrator.handleInventoryReserved(e);
    }

    /*
     * listener
     * event source: inventory service
     * purpose: confirming reservation failed while trying to reserve items in an order's
     * */
    @KafkaListener(topics = "${topics.inventory-reservation-failed:inventory.reservation.failed}",
            groupId = "${spring.kafka.consumer.group-id:saga-orchestrator}"
    )
    public void onInventoryReservationFailed(String message) throws Exception {
        InventoryReservationFailedEvent e = mapper.readValue(message, InventoryReservationFailedEvent.class);
        orchestrator.handleInventoryFailed(e);
    }


    /*
     * listener: handle inventory.released (after stock release confirmation)
     * event source: inventory service
     * purpose: confirming stock is released after an order is failed
     * compensation step in saga
     * */
    @KafkaListener(topics = "${topics.inventory-released:inventory.released}",
            groupId = "${spring.kafka.consumer.group-id:saga-orchestrator}"
    )
    public void onInventoryReleased(String message) throws Exception {
        InventoryReleasedEvent e = mapper.readValue(message, InventoryReleasedEvent.class);
        orchestrator.handleInventoryReleased(e);
    }

    /*
     * listener: handle payment.success
     * event source: payment service
     * purpose: confirming payment has been completed successfully
     * compensation step in saga
     * */
    @KafkaListener(topics = "${topics.payments-success:payment.success}",
            groupId = "${spring.kafka.consumer.group-id:saga-orchestrator}"
    )
    public void onPaymentSuccess(String message) throws Exception {
        PaymentSuccessEvent e = mapper.readValue(message, PaymentSuccessEvent.class);
        orchestrator.handlePaymentSuccess(e);
    }

    /*
     * listener: handle payment.failed
     * event source: payment service
     * purpose: confirming payment has been failed
     * */
    @KafkaListener(topics = "${topics.payments-failed:payment.failed}",
            groupId = "${spring.kafka.consumer.group-id:saga-orchestrator}"
    )
    public void onPaymentFailed(String message) throws Exception {
        PaymentFailedEvent e = mapper.readValue(message, PaymentFailedEvent.class);
        orchestrator.handlePaymentFailed(e);
    }

    /*
     * listener: handle payment.refunded
     * event source: payment service
     * purpose: confirming refund step completed successfully after an order failure
     * compensation step in saga
     * */
    @KafkaListener(topics = "${topics.payments-refunded:payments.refunded}",
            groupId = "${spring.kafka.consumer.group-id:saga-orchestrator}"
    )
    public void onPaymentRefunded(String message) throws Exception {
        PaymentRefundedEvent e = mapper.readValue(message, PaymentRefundedEvent.class);
        orchestrator.handlePaymentRefunded(e);
    }

}

