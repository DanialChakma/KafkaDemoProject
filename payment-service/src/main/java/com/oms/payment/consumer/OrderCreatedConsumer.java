//package com.oms.saga.consumer;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.oms.saga.service.PaymentService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//import java.util.UUID;
//
////Kafka Consumer (listening for orders.created events)
//
//@Component
//@RequiredArgsConstructor
//public class OrderCreatedConsumer {
//
//    private final PaymentService paymentService;
//    private final ObjectMapper mapper = new ObjectMapper();
//
//    @KafkaListener(topics = "${topics.orders-created:orders.created}",
//            groupId = "${spring.kafka.consumer.group-id:payment-service}"
//    )
//    @Transactional
//    public void consume(String message) {
//        try {
//            JsonNode json = mapper.readTree(message);
//            String orderId = json.get("orderId").asText();
//            BigDecimal total = new BigDecimal(json.get("totalAmount").asText());
//            paymentService.capturePayment(orderId, total);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}
//
