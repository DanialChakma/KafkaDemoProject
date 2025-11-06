package com.oms.inventory.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, unique = true)
    private Long productId;  // Product ID reference from Product Service

    @Column(nullable = false)
    private Integer stockQuantity;

    private Integer reservedQuantity;

    @Column(nullable = false)
    private Boolean inStock;

    private LocalDateTime lastUpdated;

    @PrePersist
    public void onCreate() {
        this.lastUpdated = LocalDateTime.now();
        this.inStock = (stockQuantity != null && stockQuantity > 0);
        this.reservedQuantity = 0;
    }

    @PreUpdate
    public void onUpdate() {
        this.lastUpdated = LocalDateTime.now();
        this.inStock = (stockQuantity != null && stockQuantity > 0);
    }
}

