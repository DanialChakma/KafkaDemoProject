package com.oms.payment.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentRequestEvent {
    private String eventId;
    private String orderId;
    private Long customerId;
    private BigDecimal totalAmount;
    private String paymentMethod;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;
}

