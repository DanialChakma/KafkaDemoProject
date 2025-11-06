package com.oms.saga.events;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductValidationResultEvent {
    private String eventId;   // for idempotency
    private String orderId;
    private Long customerId;
    private boolean valid;
    private String reason;
    private List<OrderItemDTO> items; // ✅ include validated items
}

