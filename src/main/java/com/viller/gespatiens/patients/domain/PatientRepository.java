package com.viller.gespatiens.patients.domain;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PatientRepository {
    Patient save(Patient patient);
    Optional<Patient> findById(UUID id);
    boolean existsByDni(Dni dni);
    boolean existsByVisitCode(String visitCode);
    // Números de referencia en uso por los residentes activos
    Set<Integer> findOccupiedNumbers();
}
