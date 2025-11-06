package com.oms.inventory.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ItemStatusDto {
    private Long productId;
    private String sku;
    private Integer quantity;
    private BigDecimal unitPrice;
    private String result; // SUCCESS or FAILED
}
