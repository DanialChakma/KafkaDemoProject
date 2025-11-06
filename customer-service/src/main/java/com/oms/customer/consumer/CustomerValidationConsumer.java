package com.oms.customer.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.customer.events.CustomerValidationResultEvent;
import com.oms.customer.events.OrderValidationRequestEvent;
import com.oms.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerValidationConsumer {

    private final CustomerRepository customerRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper mapper;

    @Value("${topics.customer-validation-result:customer.validation.result}")
    private String customerValidationResultTopic;

    @KafkaListener(topics = "${topics.order-validation-request:order.validation.request}",
            groupId = "customer-service")
    public void onOrderValidationRequest(String message) throws Exception {
        OrderValidationRequestEvent event = mapper.readValue(message, OrderValidationRequestEvent.class);
        boolean exists = customerRepository.existsById(event.getCustomerId());

        CustomerValidationResultEvent result = new CustomerValidationResultEvent(
                event.getOrderId(),
                event.getCustomerId(),
                exists,
                event.getEventId()
        );

        kafkaTemplate.send(customerValidationResultTopic, result);
        log.info("✅ Sent CustomerValidationResult for orderId={} success={}", event.getOrderId(), exists);
    }
}
