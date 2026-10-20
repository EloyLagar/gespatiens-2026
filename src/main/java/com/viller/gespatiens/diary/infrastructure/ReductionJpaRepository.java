package com.viller.gespatiens.diary.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReductionJpaRepository extends JpaRepository<ReductionJpaEntity, UUID> {
    List<ReductionJpaEntity> findByDate(LocalDate date);
    Optional<ReductionJpaEntity> findByDateAndPatientId(LocalDate date, UUID patientId);
}
