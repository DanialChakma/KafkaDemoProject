package com.oms.product.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderValidationRequestEvent {
    private String orderId;
    private Long customerId;
    private List<Long> productIds;
    private List<OrderItemDTO> items;
    private String eventId;  // for idempotency
}

