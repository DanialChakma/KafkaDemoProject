package com.oms.events;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class OrderValidationRequestEvent {
    private String orderId; // String UUID
    private Long customerId;
    private List<OrderItemDto> items;
    private String eventId;  // for idempotency
}
