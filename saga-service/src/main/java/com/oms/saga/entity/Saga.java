package com.oms.saga.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "saga")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Saga {

    @Id
    @Column( name = "order_id", length = 36)
    private String orderId; // store UUID as string

    @Column( name = "inventory_reserved" )
    private boolean inventoryReserved;

    @Column( name = "payment_succeeded" )
    private boolean paymentSucceeded;

    @Column( name = "status" )
    private String status; // PENDING, COMPLETED, FAILED

    @Column( name = "last_updated" )
    private Instant lastUpdated;

    // ✅ Track processed event IDs for idempotency
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "saga_processed_events", joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "event_id")
    @Builder.Default
    private Set<String> processedEventIds = new HashSet<>();

    public boolean hasProcessed(String eventId) {
        return processedEventIds != null && processedEventIds.contains(eventId);
    }

    public void markProcessed(String eventId) {
        if (processedEventIds == null) processedEventIds = new HashSet<>();
        processedEventIds.add(eventId);
    }

}
