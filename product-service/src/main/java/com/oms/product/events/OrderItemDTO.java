package com.oms.product.events;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemDTO {
    private Long productId;
    private String sku;
    private Integer quantity;
    private BigDecimal unitPrice;
}

