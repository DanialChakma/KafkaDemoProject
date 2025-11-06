package com.oms.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCapturedEvent {
    private String orderId;
    private String paymentReference;
    private Double amount;
}

