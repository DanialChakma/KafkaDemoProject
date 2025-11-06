package com.oms.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_event")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column( name = "aggregate_id" )
    private String aggregateId; // e.g. orderId

    @Column( name = "aggregate_type" )
    private String aggregateType; // e.g. "Order"

    @Column( name = "event_type" )
    private String eventType; // e.g. "orders.created"

    @Column( name="payload", columnDefinition = "TEXT")
    @Lob
    private String payload;

    @Column( name="is_published" )
    @Builder.Default
    private boolean published = false;

    @Builder.Default
    @Column( name = "created_at" )
    private Instant createdAt = Instant.now();

    @Column( name = "updated_at" )
    private Instant updatedAt;

}

