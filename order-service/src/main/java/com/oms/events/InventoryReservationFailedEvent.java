package com.oms.events;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservationFailedEvent {
    private String orderId; // UUID of type string
    private Long productId;
    private Integer requiredQty;
    private String reason;
}

