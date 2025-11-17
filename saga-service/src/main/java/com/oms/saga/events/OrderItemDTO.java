package com.oms.saga.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderItemDTO {
    @JsonProperty("productId")
    private Long productId;
    @JsonProperty("sku")
    private String sku;
    @JsonProperty("quantity")
    private Integer quantity;
    @JsonProperty("unitPrice")
    private BigDecimal unitPrice;
}

