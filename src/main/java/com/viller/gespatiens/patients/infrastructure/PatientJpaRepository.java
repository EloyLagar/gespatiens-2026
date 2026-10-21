package com.viller.gespatiens.patients.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface PatientJpaRepository extends JpaRepository<PatientJpaEntity, UUID> {
    boolean existsByDni(String dni);
    boolean existsByVisitCode(String visitCode);
    boolean existsByDniAndIdNot(String dni, UUID id);
    boolean existsByVisitCodeAndIdNot(String visitCode, UUID id);
    List<PatientJpaEntity> findByExitDateIsNullOrderByNumber();
    List<PatientJpaEntity> findByExitDateIsNotNullOrderByExitDateDesc();

    @Query("select p.number from PatientJpaEntity p where p.number is not null")
    List<Integer> findOccupiedNumbers();
}
