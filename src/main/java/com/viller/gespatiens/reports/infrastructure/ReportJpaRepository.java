package com.viller.gespatiens.reports.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportJpaRepository extends JpaRepository<ReportJpaEntity, UUID> {

    List<ReportJpaEntity> findByPatientIdOrderByCreatedAtAsc(UUID patientId);
}
