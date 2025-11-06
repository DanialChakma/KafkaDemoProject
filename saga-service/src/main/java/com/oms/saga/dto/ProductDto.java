package com.oms.saga.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
//    private String productId;
    @JsonProperty("productCode")
    private String productCode;
    @JsonProperty("ProductName")
    private String ProductName;
    @JsonProperty("price")
    private double price;

}
