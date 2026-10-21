package com.viller.gespatiens.audit.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

// Specification: los filtros de la pantalla son opcionales y se combinan
public interface AuditEntryJpaRepository extends JpaRepository<AuditEntryJpaEntity, UUID>,
        JpaSpecificationExecutor<AuditEntryJpaEntity> {
}
