package com.oms.inventory.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InventoryReservationResultEvent {
    private String eventId;
    private String orderId;
    private Long customerId;
    private String status; // RESERVED or FAILED
    private String reason; // for failure cause
    private List<ItemStatusDto> items;
}

