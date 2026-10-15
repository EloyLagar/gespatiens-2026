package com.viller.gespatiens.patients.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface PatientJpaRepository extends JpaRepository<PatientJpaEntity, UUID> {
    boolean existsByDni(String dni);

    @Query("select p.number from PatientJpaEntity p where p.number is not null")
    List<Integer> findOccupiedNumbers();
}
