package com.oms.saga.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRefundedEvent {
    private String eventId = UUID.randomUUID().toString();
    private String orderId;
    private String refundId; // optional, could be a UUID from payment service
    private String reason;
}

