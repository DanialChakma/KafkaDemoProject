package com.oms.customer.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.customer.events.OrderValidationRequestEvent;
import com.oms.customer.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerValidationConsumer {
    private final CustomerService customerService;
    private final ObjectMapper mapper;

    @KafkaListener(topics = "${topics.order-validation-request:order.validation.request}",
            groupId = "customer-service")
    public void onOrderValidationRequest(String message) throws Exception {
        OrderValidationRequestEvent event = mapper.readValue(message, OrderValidationRequestEvent.class);
        customerService.handleOrderValidationRequest(event);
    }

}
