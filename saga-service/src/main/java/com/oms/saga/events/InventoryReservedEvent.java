package com.oms.saga.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class InventoryReservedEvent {
    @JsonProperty("eventId")
    private String eventId;
    @JsonProperty("orderId")
    private String orderId;   // UUID string
    private Long productId;
    @JsonProperty("customerId")
    private Long customerId;
    @JsonProperty("reservedQty")
    private Integer reservedQty;
    @JsonProperty("status")
    private String status;
    @JsonProperty("items")
    private List<OrderItemDTO> items; // ✅ include validated items
}
