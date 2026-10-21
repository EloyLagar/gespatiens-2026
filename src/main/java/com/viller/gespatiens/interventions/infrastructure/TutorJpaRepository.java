package com.viller.gespatiens.interventions.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TutorJpaRepository extends JpaRepository<TutorJpaEntity, TutorJpaEntity.Key> {
    List<TutorJpaEntity> findByPatientId(UUID patientId);
    void deleteByPatientId(UUID patientId);
}
