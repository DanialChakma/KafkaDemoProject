//package com.oms.model;
//
//
//import jakarta.persistence.*;
//import java.math.BigDecimal;
//import java.time.OffsetDateTime;
//import java.util.UUID;
//
//@Entity
//@Table(name = "payments", indexes = {
//        @Index(name = "idx_payments_order", columnList = "order_id")
//})
//public class Payment {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.UUID)
//    private UUID id;
//
//    @Column(name = "order_id", nullable = false)
//    private UUID orderId;
//
//    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
//    private BigDecimal amount;
//
//    @Column(name = "currency", length = 3)
//    private String currency;
//
//    @Column(name = "provider", length = 64)
//    private String provider;
//
//    @Column(name = "payment_status", length = 30)
//    private String paymentStatus;
//
//    @Column(name = "provider_tx_id", length = 100)
//    private String providerTxId;
//
//    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
//    private OffsetDateTime createdAt = OffsetDateTime.now();
//
//    // Getters and Setters
//}
//
