package com.viller.gespatiens.patients.application;

import com.viller.gespatiens.patients.domain.Patient;
import com.viller.gespatiens.patients.domain.PatientAlreadyRegisteredException;
import com.viller.gespatiens.patients.domain.PatientDetails;
import com.viller.gespatiens.patients.domain.PatientNotFoundException;
import com.viller.gespatiens.patients.domain.PatientRepository;
import com.viller.gespatiens.patients.domain.VisitCodeAlreadyInUseException;

import java.util.UUID;

public class UpdatePatientDetailsUseCase {

    private final PatientRepository patientRepository;

    public UpdatePatientDetailsUseCase(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public Patient execute(UUID patientId, PatientDetails details) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException(patientId));
        // Se excluye al propio residente: guardar su ficha sin cambiar el DNI no es un duplicado
        if (patientRepository.existsByDniAndIdNot(details.dni(), patientId)) {
            throw new PatientAlreadyRegisteredException(details.dni());
        }
        if (details.visitCode() != null && patientRepository.existsByVisitCodeAndIdNot(details.visitCode(), patientId)) {
            throw new VisitCodeAlreadyInUseException(details.visitCode());
        }
        patient.updateDetails(details);
        return patientRepository.save(patient);
    }
}
