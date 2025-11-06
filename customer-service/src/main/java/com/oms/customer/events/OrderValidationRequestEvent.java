package com.oms.customer.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderValidationRequestEvent {
    private String orderId;
    private Long customerId;
    private List<Long> productIds;
    private String eventId;  // for idempotency
}

