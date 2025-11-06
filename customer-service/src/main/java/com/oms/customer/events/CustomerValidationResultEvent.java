package com.oms.customer.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerValidationResultEvent {
    private String orderId;
    private Long customerId;
    private boolean valid;
    private String eventId;
}

