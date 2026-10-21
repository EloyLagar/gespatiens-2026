package com.viller.gespatiens.patients.application;

import com.viller.gespatiens.patients.domain.Patient;
import com.viller.gespatiens.patients.domain.PatientNotFoundException;
import com.viller.gespatiens.patients.domain.PatientRepository;
import com.viller.gespatiens.patients.domain.StayEvents;

import java.time.LocalDate;
import java.util.UUID;

public class DischargePatientUseCase {

    private final PatientRepository patientRepository;
    private final StayEvents stayEvents;

    public DischargePatientUseCase(PatientRepository patientRepository, StayEvents stayEvents) {
        this.patientRepository = patientRepository;
        this.stayEvents = stayEvents;
    }

    public Patient execute(UUID patientId, LocalDate exitDate) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException(patientId));
        // Lanza PatientAlreadyDischargedException si ya era un residente antiguo
        patient.discharge(exitDate);
        Patient saved = patientRepository.save(patient);
        stayEvents.stayEnded(saved.getId(), saved.getEntryDate(), saved.getExitDate());
        return saved;
    }
}
