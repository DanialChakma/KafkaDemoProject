package com.oms.customer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    @JsonProperty("orderId")
    private String orderId;
    @JsonProperty("customerName")
    private String customerName;
    @JsonProperty("items")
    private List<ProductDto> items = new ArrayList<>();
    @JsonProperty("totalAmount")
    private double totalAmount;
}
