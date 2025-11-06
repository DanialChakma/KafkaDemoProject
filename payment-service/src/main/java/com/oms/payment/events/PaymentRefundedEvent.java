package com.oms.payment.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRefundedEvent {
    private String eventId;
    private String orderId;
    private String refundId; // optional, could be a UUID from payment service
    private String reason;
}

