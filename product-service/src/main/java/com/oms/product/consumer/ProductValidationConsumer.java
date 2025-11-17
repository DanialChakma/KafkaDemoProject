package com.oms.product.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.product.events.OrderValidationRequestEvent;
import com.oms.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductValidationConsumer {

    private final ObjectMapper mapper;
    private final ProductService productService;

    @KafkaListener(topics = "${topics.order-validation-request:order.validation.request}",
            groupId = "product-service")
    public void onOrderValidationRequest(String message) throws Exception {
        OrderValidationRequestEvent event = mapper.readValue(message, OrderValidationRequestEvent.class);
        productService.handleOrderValidationRequest(event);
    }

}

