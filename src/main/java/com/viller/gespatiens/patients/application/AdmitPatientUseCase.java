package com.viller.gespatiens.patients.application;

import com.viller.gespatiens.patients.domain.Patient;
import com.viller.gespatiens.patients.domain.PatientAlreadyRegisteredException;
import com.viller.gespatiens.patients.domain.PatientDetails;
import com.viller.gespatiens.patients.domain.PatientRepository;
import com.viller.gespatiens.patients.domain.StayEvents;
import com.viller.gespatiens.patients.domain.ResidentNumberAssigner;
import com.viller.gespatiens.patients.domain.VisitCodeAlreadyInUseException;

import java.time.LocalDate;

public class AdmitPatientUseCase {

    private final PatientRepository patientRepository;
    private final ResidentNumberAssigner numberAssigner;
    private final StayEvents stayEvents;

    public AdmitPatientUseCase(PatientRepository patientRepository, ResidentNumberAssigner numberAssigner,
                               StayEvents stayEvents) {
        this.patientRepository = patientRepository;
        this.numberAssigner = numberAssigner;
        this.stayEvents = stayEvents;
    }

    public Patient execute(PatientDetails details, LocalDate entryDate) {
        if (patientRepository.existsByDni(details.dni())) {
            throw new PatientAlreadyRegisteredException(details.dni());
        }
        if (details.visitCode() != null && patientRepository.existsByVisitCode(details.visitCode())) {
            throw new VisitCodeAlreadyInUseException(details.visitCode());
        }
        // Lanza CenterFullException si ya hay 32 residentes activos
        int number = numberAssigner.assignLowestFree(patientRepository.findOccupiedNumbers());

        Patient saved = patientRepository.save(Patient.admit(details, number, entryDate));
        stayEvents.stayStarted(saved.getId(), saved.getEntryDate());
        return saved;
    }
}
