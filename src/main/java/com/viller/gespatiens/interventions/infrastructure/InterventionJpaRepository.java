package com.viller.gespatiens.interventions.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface InterventionJpaRepository extends JpaRepository<InterventionJpaEntity, UUID> {

    List<InterventionJpaEntity> findByPatientIdOrderByDateDescCreatedAtDesc(UUID patientId);

    @Query("SELECT i.patientId AS patientId, COUNT(i) AS total, MAX(i.date) AS latest "
            + "FROM InterventionJpaEntity i GROUP BY i.patientId")
    List<PatientStats> statsByPatient();

    interface PatientStats {
        UUID getPatientId();
        long getTotal();
        LocalDate getLatest();
    }
}
