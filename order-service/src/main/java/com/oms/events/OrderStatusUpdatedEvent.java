package com.oms.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdatedEvent {
    private String eventId;
    private String orderId;
    private String status; // CONFIRMED, CANCELLED, etc.
    private String reason;
    private Instant timestamp;
}

