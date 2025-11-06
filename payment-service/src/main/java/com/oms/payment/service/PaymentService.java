package com.oms.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.payment.entity.OutboxEvent;
import com.oms.payment.entity.Payment;
import com.oms.payment.entity.PaymentMethod;
import com.oms.payment.entity.PaymentStatus;
import com.oms.payment.events.*;
import com.oms.payment.repository.OutboxEventRepository;
import com.oms.payment.repository.PaymentRepository;
import com.oms.payment.utils.RefGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    private final OutboxEventRepository outboxRepository;

    private final ObjectMapper objectMapper;

    private final KafkaTemplate<String, Object> kafkaTemplate;


    @Value("${topics.payments-success:payments.success}")
    private String PAYMENT_SUCCESS_TOPIC;

    @Value("${topics.payments-failed:payments.failed}")
    private String PAYMENT_FAILURE_TOPIC;

    @Value("${topics.payments-refunded:payments.refunded}")
    private String PAYMENT_REFUNDED_TOPIC;

    @Transactional
    public Payment processPayment(Payment payment) {
        payment.setStatus(PaymentStatus.SUCCESS); // simulate payment gateway
        return paymentRepository.save(payment);
    }

    public Optional<Payment> getPaymentByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    public Optional<Payment> getPaymentByReference(String ref) {
        return paymentRepository.findByPaymentReference(ref);
    }

    @Transactional
    public void updateStatus(String ref, PaymentStatus status) {
        paymentRepository.findByPaymentReference(ref).ifPresent(payment -> {
            payment.setStatus(status);
            paymentRepository.save(payment);
        });
    }



    @Transactional
    public void capturePayment(PaymentRequestEvent event) throws JsonProcessingException {
        PaymentFailedEvent failedEvent = null;
        PaymentSuccessEvent successEvent = null;
        // Simulate payment processing
        boolean failPayment = ThreadLocalRandom.current().nextInt(100) < 25; // 25% chance to fail

        String paymentRef = RefGenerator.genPaymentReference();

        Payment payment =  Payment.builder()
                .orderId(event.getOrderId())
                .amount(event.getTotalAmount())
                .currency("USD")
                .method(PaymentMethod.valueOf(event.getPaymentMethod()))
                .paymentReference(paymentRef)
                .build();


        if (failPayment) {
            payment.setStatus(PaymentStatus.FAILED);
            failedEvent = PaymentFailedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(event.getOrderId())
                    .reason("Insufficient Amount:%s".formatted(event.getTotalAmount()))
                    .build();
        } else {
            payment.setStatus(PaymentStatus.SUCCESS);
            successEvent = PaymentSuccessEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .paymentReference(paymentRef)
                    .orderId(event.getOrderId())
                    .amount(event.getTotalAmount())
                    .build();
        }

        paymentRepository.save(payment);

        if (failPayment) {
            this.publishEvent(failedEvent, PAYMENT_FAILURE_TOPIC, event.getOrderId(), "payment-service");
        }else{
            this.publishEvent(successEvent, PAYMENT_SUCCESS_TOPIC, event.getOrderId(), "payment-service");
        }

    }

    @Transactional
    public void handleRefundRequest(PaymentRefundEvent event) throws JsonProcessingException {

        // Mark existing payment as REFUNDED

        Payment payment = paymentRepository.findByOrderId(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("Payment not found for order " + event.getOrderId()));

        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        //Build refund confirmation event
        PaymentRefundedEvent refundedEvent = new PaymentRefundedEvent(
                UUID.randomUUID().toString(), // event id
                event.getOrderId(),
                UUID.randomUUID().toString(), // refund ID
                event.getReason()
        );

        // Publish via Kafka
        this.publishEvent(refundedEvent, PAYMENT_REFUNDED_TOPIC, event.getOrderId(), "payment-service");

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

