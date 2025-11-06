package com.oms.payment.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments",
indexes = {
        @Index(name = "idx_payments_order", columnList = "order_id")
}
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "payment_reference", nullable = false, unique = true)
    private String paymentReference;  // internal reference or external txn id

    @Column(name = "order_id", nullable = false, length = 36)
    private String orderId;  // foreign key reference (Order Service ID)

    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 30)
    private PaymentStatus status;

    @Column(name = "transaction_id")
    private String transactionId; // from gateway

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "provider", length = 64)
    private String provider;

    @Column(name = "provider_tx_id", length = 100)
    private String providerTxId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", updatable = true)
    @Builder.Default
    private LocalDateTime updatedAt =  LocalDateTime.now();

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.status = PaymentStatus.PENDING;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

