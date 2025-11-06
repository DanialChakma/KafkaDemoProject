package com.oms.saga.events;

import lombok.Data;

@Data
public class PaymentSuccessEvent {
    private String eventId;
    private String orderId;
    private String paymentReference;
    private Double amount;
}
