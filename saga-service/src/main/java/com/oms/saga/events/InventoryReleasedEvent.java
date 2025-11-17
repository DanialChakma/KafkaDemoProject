package com.oms.saga.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oms.saga.dto.InventoryItemDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
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

