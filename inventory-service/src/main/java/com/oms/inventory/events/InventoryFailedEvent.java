package com.oms.inventory.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryFailedEvent {
    private UUID orderId;
    private Long productId;
    private String reason;
    private String eventId;
}

