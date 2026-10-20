package com.viller.gespatiens.patients.application;

import com.viller.gespatiens.patients.domain.Patient;
import com.viller.gespatiens.patients.domain.PatientNotFoundException;
import com.viller.gespatiens.patients.domain.PatientRepository;

import java.time.LocalDate;
import java.util.UUID;

public class DischargePatientUseCase {

    private final PatientRepository patientRepository;

    public DischargePatientUseCase(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public Patient execute(UUID patientId, LocalDate exitDate) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException(patientId));
        // Lanza PatientAlreadyDischargedException si ya era un residente antiguo
        patient.discharge(exitDate);
        return patientRepository.save(patient);
    }
}
