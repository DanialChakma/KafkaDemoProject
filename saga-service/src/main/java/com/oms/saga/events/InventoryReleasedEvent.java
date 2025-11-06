package com.oms.saga.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReleasedEvent {
    private String eventId;
    private String orderId;
    private String reason;
}

