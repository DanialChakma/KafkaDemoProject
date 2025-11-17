package com.oms.saga.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentFailedEvent {
    @JsonProperty("eventId")
    private String eventId;
    @JsonProperty("orderId")
    private String orderId;
    @JsonProperty("reason")
    private String reason;
}
