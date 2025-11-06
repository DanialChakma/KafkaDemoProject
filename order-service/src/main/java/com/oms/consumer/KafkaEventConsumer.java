package com.oms.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.events.*;
import com.oms.repository.OrderRepository;
import com.oms.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@RequiredArgsConstructor
public class KafkaEventConsumer {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private final OrderService orderService;

    @KafkaListener(
            topics = "${topics.order-cancelled:orders.cancelled}",
            groupId = "${spring.kafka.consumer.group-id:order-service}"
    )
    @Transactional
    public void onOrderCancelled(String message) throws Exception {
        OrderCancelledEvent event = objectMapper.readValue(message, OrderCancelledEvent.class);
        orderService.handleOrderCancelledEvent(event);
    }

    @KafkaListener(
            topics = "${topics.order-status-updated:orders.status.updated}",
            groupId = "${spring.kafka.consumer.group-id:order-service}"
    )
    @Transactional
    public void onOrderStatusUpdated(String message) throws Exception {
        OrderStatusUpdatedEvent event = objectMapper.readValue(message, OrderStatusUpdatedEvent.class);
        orderService.handleOrderStateChangeEvent(event);
    }

}
