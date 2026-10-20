package com.viller.gespatiens.patients.application;

import com.viller.gespatiens.patients.domain.Patient;
import com.viller.gespatiens.patients.domain.PatientRepository;
import com.viller.gespatiens.patients.domain.PatientStatus;

import java.util.List;

public class ListPatientsUseCase {

    private final PatientRepository patientRepository;

    public ListPatientsUseCase(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public List<Patient> execute(PatientStatus status) {
        return patientRepository.findByStatus(status);
    }
}
