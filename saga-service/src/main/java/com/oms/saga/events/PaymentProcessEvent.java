package com.oms.saga.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentProcessEvent {
    private String eventId;
    private String orderId;
    private Long customerId;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private Instant createdAt;
}

