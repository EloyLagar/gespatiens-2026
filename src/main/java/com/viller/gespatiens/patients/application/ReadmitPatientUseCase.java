package com.viller.gespatiens.patients.application;

import com.viller.gespatiens.patients.domain.Patient;
import com.viller.gespatiens.patients.domain.PatientAlreadyActiveException;
import com.viller.gespatiens.patients.domain.PatientNotFoundException;
import com.viller.gespatiens.patients.domain.PatientRepository;
import com.viller.gespatiens.patients.domain.StayEvents;
import com.viller.gespatiens.patients.domain.ResidentNumberAssigner;

import java.time.LocalDate;
import java.util.UUID;

public class ReadmitPatientUseCase {

    private final PatientRepository patientRepository;
    private final ResidentNumberAssigner numberAssigner;
    private final StayEvents stayEvents;

    public ReadmitPatientUseCase(PatientRepository patientRepository, ResidentNumberAssigner numberAssigner,
                                 StayEvents stayEvents) {
        this.patientRepository = patientRepository;
        this.numberAssigner = numberAssigner;
        this.stayEvents = stayEvents;
    }

    public Patient execute(UUID patientId, LocalDate entryDate) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException(patientId));
        // Se comprueba antes de pedir número: si el centro está lleno, un residente
        // que ya está dentro debe recibir este error y no el de centro lleno
        if (patient.isActive()) {
            throw new PatientAlreadyActiveException(patientId);
        }
        // RF10: tampoco se puede reincorporar a un antiguo si ya hay 32 residentes activos
        int number = numberAssigner.assignLowestFree(patientRepository.findOccupiedNumbers());

        patient.readmit(number, entryDate);
        Patient saved = patientRepository.save(patient);
        stayEvents.stayStarted(saved.getId(), saved.getEntryDate());
        return saved;
    }
}
