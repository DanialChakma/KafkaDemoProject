package com.oms.payment.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.payment.events.PaymentRefundEvent;
import com.oms.payment.events.PaymentRequestEvent;
import com.oms.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventListener {

    private final PaymentService paymentService;
    private final ObjectMapper mapper = new ObjectMapper();

    @KafkaListener(
            topics = "${topics.payments-request:payments.request}",
            groupId = "${spring.kafka.consumer.group-id:payment-service}"
    )
    @Transactional
    public void handlePaymentRequest(String message) {
        try {
            /*
            JsonNode json = mapper.readTree(message);
            String orderId = json.get("orderId").asText();
            BigDecimal total = new BigDecimal(json.get("totalAmount").asText());
            */
            PaymentRequestEvent event = mapper.readValue(message, PaymentRequestEvent.class);
            paymentService.capturePayment(event);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @KafkaListener(
            topics = "${topics.payments-refund:payments.refund}",
            groupId = "${spring.kafka.consumer.group-id:payment-service}"
    )
    @Transactional
    public void handleRefundRequest(String message) {
        try {

            PaymentRefundEvent event = mapper.readValue(message, PaymentRefundEvent.class);
            paymentService.handleRefundRequest(event);

        } catch (Exception e) {
            log.error("Failed to process refund: {}", e.getMessage(), e);
        }
    }

}
