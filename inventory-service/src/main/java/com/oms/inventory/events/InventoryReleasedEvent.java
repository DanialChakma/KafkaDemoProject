package com.oms.inventory.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oms.inventory.dto.InventoryItemDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReleasedEvent {
    @JsonProperty("orderId")
    private String orderId;
    @JsonProperty("eventId")
    private String eventId;
    @JsonProperty("reason")
    private String reason;
    @JsonProperty("items")
    private List<InventoryItemDto> releasedItems;
}

