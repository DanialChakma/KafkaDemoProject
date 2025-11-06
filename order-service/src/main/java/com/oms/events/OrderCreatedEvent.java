package com.oms.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {
    private UUID orderId;
    private Long customerId;
    private List<OrderItemDto> items;
    private BigDecimal totalAmount;
    private String currency;
    private String idempotencyKey; // optional
}

