package com.viller.gespatiens.patients.application;

import com.viller.gespatiens.patients.domain.Dni;
import com.viller.gespatiens.patients.domain.Patient;
import com.viller.gespatiens.patients.domain.PatientRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// Repositorio en memoria compartido por los tests de los casos de uso de residentes
class InMemoryPatientRepository implements PatientRepository {

    private final Map<UUID, Patient> patients = new HashMap<>();

    @Override
    public Patient save(Patient patient) {
        patients.put(patient.getId(), patient);
        return patient;
    }

    @Override
    public Optional<Patient> findById(UUID id) {
        return Optional.ofNullable(patients.get(id));
    }

    @Override
    public boolean existsByDni(Dni dni) {
        return patients.values().stream().anyMatch(p -> p.getDetails().dni().equals(dni));
    }

    @Override
    public boolean existsByVisitCode(String visitCode) {
        return patients.values().stream().anyMatch(p -> visitCode.equals(p.getDetails().visitCode()));
    }

    @Override
    public Set<Integer> findOccupiedNumbers() {
        return patients.values().stream()
                .filter(Patient::isActive)
                .map(Patient::getNumber)
                .collect(Collectors.toSet());
    }

    Patient findByNumber(int number) {
        return patients.values().stream()
                .filter(p -> Integer.valueOf(number).equals(p.getNumber()))
                .findFirst().orElseThrow();
    }
}
