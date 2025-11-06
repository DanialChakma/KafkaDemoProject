package com.oms.saga.repository;

import com.oms.saga.entity.SagaMessageLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SagaMessageLogRepository extends JpaRepository<SagaMessageLog, Long> {
    Optional<SagaMessageLog> findByEventId(String eventId);
}
