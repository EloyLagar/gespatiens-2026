package com.viller.gespatiens.patients.application;

import com.viller.gespatiens.patients.domain.Patient;
import com.viller.gespatiens.patients.domain.PatientNotFoundException;
import com.viller.gespatiens.patients.domain.PatientRepository;

import java.util.UUID;

public class DeletePatientUseCase {

    private final PatientRepository patientRepository;

    public DeletePatientUseCase(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public void execute(UUID patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException(patientId));
        // Lanza PatientStillActiveException si el residente sigue en el centro
        patient.ensureCanBeDeleted();
        patientRepository.delete(patient);
    }
}
