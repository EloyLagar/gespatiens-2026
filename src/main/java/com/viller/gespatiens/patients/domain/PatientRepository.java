package com.viller.gespatiens.patients.domain;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PatientRepository {
    Patient save(Patient patient);
    Optional<Patient> findById(UUID id);
    // Activos ordenados por número; antiguos del que salió más recientemente al más antiguo
    List<Patient> findByStatus(PatientStatus status);
    boolean existsByDni(Dni dni);
    boolean existsByVisitCode(String visitCode);
    // Números de referencia en uso por los residentes activos
    Set<Integer> findOccupiedNumbers();
}
