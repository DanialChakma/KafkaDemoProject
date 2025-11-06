package com.oms.saga.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdatedEvent {
    private String eventId;
    private String orderId; // UUID of type String, length 36
    private String status; // CONFIRMED, CANCELLED, etc.
    private String reason;
    private Instant timestamp;
}

