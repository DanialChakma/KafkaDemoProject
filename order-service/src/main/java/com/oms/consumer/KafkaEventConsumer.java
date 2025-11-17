package com.oms.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.events.*;
import com.oms.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@RequiredArgsConstructor
@Component
public class KafkaEventConsumer {
    private final ObjectMapper objectMapper;
    private final OrderService orderService;

    @KafkaListener(
            topics = "${topics.order-cancelled:orders.cancelled}",
            groupId = "${spring.kafka.consumer.group-id:order-service}-cancelled"
    )
    @Transactional
    public void onOrderCancelled(String message) throws Exception {
        log.info("Order Status Cancelled Arrived:{}", message);
        OrderCancelledEvent event = objectMapper.readValue(message, OrderCancelledEvent.class);
        orderService.handleOrderCancelledEvent(event);
    }

    @KafkaListener(
            topics = "${topics.order-status-updated:orders.status.updated}",
            groupId = "${spring.kafka.consumer.group-id:order-service}-update-status"
    )
    @Transactional
    public void onOrderStatusUpdated(String message) throws Exception {
        log.info("Order Status update Arrived:{}", message);
        OrderStatusUpdatedEvent event = objectMapper.readValue(message, OrderStatusUpdatedEvent.class);
        orderService.handleOrderStateChangeEvent(event);
    }

}
