package com.oms.saga.events;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Setter
@Getter
@Builder
public class InventoryReservedEvent {
    private String eventId;
    private String orderId;   // UUID string
    private Long productId;
    private Long customerId;
    private Integer reservedQty;
    private List<OrderItemDTO> items; // ✅ include validated items
}
