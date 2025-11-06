package com.oms.saga.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "saga_message_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaMessageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false, unique = true)
    private String eventId;  // UUID from the event itself

    @Column(nullable = false)
    private String sagaId;   // Usually your orderId

    @Column(nullable = false)
    private String eventType;  // e.g. "PaymentSuccessEvent"

    @Lob
    @Column(columnDefinition = "TEXT")
    private String payload;   // Full JSON message for debugging

    @Column(nullable = false)
    private Instant receivedAt; // When this message was received

    @Column(nullable = false)
    private String sourceService; // e.g., "inventory-service" or "payment-service"

    @Column(nullable = false)
    private String status; // e.g., "PROCESSED", "IGNORED_DUPLICATE", "FAILED"
}

