package com.oms.saga.events;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;
}

