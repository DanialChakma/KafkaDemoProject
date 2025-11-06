//package com.oms.saga.consumer;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.oms.saga.service.PaymentService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class PaymentRefundConsumer {
//
//    private final ObjectMapper mapper;
//    private final PaymentService paymentService;
//
//    @KafkaListener(topics = "${topics.payments-refund:payments.refund}", groupId = "payment-service")
//    @Transactional
//    public void handleRefundRequest(String message) {
//        try {
//            JsonNode json = mapper.readTree(message);
//            String orderId = json.get("orderId").asText();
//            String reason = json.has("reason") ? json.get("reason").asText() : "N/A";
//
//            paymentService.handleRefundRequest(orderId, reason);
//
//            log.info("✅ Payment refund confirmed for order {} (reason: {})", orderId, reason);
//
//        } catch (Exception e) {
//            log.error("❌ Failed to process refund: {}", e.getMessage(), e);
//        }
//    }
//}
//
