package com.oms.saga.events;

import lombok.Data;

@Data
public class InventoryReservationFailedEvent {
    private String eventId;
    private String orderId;
    private Long productId;
    private Integer requiredQty;
    private String reason;
}
