package com.viller.gespatiens.audit.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

// Specification: los filtros de la pantalla son opcionales y se combinan
public interface AuditEntryJpaRepository extends JpaRepository<AuditEntryJpaEntity, UUID>,
        JpaSpecificationExecutor<AuditEntryJpaEntity> {

    @Modifying
    @Transactional
    @Query("DELETE FROM AuditEntryJpaEntity e WHERE e.occurredAt < :limit")
    int deleteOlderThan(Instant limit);
}
