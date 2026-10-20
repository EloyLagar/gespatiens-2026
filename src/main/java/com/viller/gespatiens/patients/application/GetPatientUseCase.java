package com.viller.gespatiens.patients.application;

import com.viller.gespatiens.patients.domain.Patient;
import com.viller.gespatiens.patients.domain.PatientNotFoundException;
import com.viller.gespatiens.patients.domain.PatientRepository;

import java.util.UUID;

public class GetPatientUseCase {

    private final PatientRepository patientRepository;

    public GetPatientUseCase(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public Patient execute(UUID patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException(patientId));
    }
}
