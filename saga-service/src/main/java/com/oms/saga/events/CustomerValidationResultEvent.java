package com.oms.saga.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerValidationResultEvent {
    private String orderId;
    private Long customerId;
    private boolean valid;
    private String eventId;
}

