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
public class InventoryReserveRequestEvent {
    private String eventId;
    private String orderId;
    private Long customerId;
    private List<OrderItemDTO> items; // ✅ include validated items
}

