package com.oms.product.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.product.events.OrderItemDTO;
import com.oms.product.events.OrderValidationRequestEvent;
import com.oms.product.events.ProductValidationResultEvent;
import com.oms.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductValidationConsumer {

    private final ProductRepository productRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper mapper;

    @Value("${product-validation-result:product.validation.result}")
    private String productValidationResultTopic;

    @KafkaListener(topics = "${topics.order-validation-request:order.validation.request}",
            groupId = "product-service")
    public void onOrderValidationRequest(String message) throws Exception {
        OrderValidationRequestEvent event = mapper.readValue(message, OrderValidationRequestEvent.class);
        boolean allExist = event.getItems().stream().map(OrderItemDTO::getProductId)
                .allMatch(productRepository::existsById);


        ProductValidationResultEvent result = ProductValidationResultEvent.builder()
                .eventId(event.getEventId())
                .valid(allExist)
                .orderId(event.getOrderId())
                .customerId(event.getCustomerId())
//                .reason()
                .items(event.getItems())
                .build();


        kafkaTemplate.send(productValidationResultTopic, result);
        log.info("✅ Sent ProductValidationResult for orderId={} valid={}", event.getOrderId(), allExist);
    }
}

