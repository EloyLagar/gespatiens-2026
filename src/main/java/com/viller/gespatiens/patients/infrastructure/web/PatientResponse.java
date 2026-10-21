package com.viller.gespatiens.patients.infrastructure.web;

import com.viller.gespatiens.patients.domain.Patient;
import com.viller.gespatiens.patients.domain.PatientDetails;

import java.time.LocalDate;
import java.util.UUID;

// DTO de salida: aplana el agregado para el JSON
public record PatientResponse(
        UUID id,
        Integer number,
        boolean active,
        String name,
        String surname,
        String dni,
        LocalDate birthDate,
        String address,
        String phoneNumber,
        String sip,
        String visitCode,
        String belongings,
        String abuseSubstances,
        String extraInfo,
        LocalDate entryDate,
        LocalDate exitDate) {

    public static PatientResponse from(Patient patient) {
        PatientDetails details = patient.getDetails();
        return new PatientResponse(
                patient.getId(),
                patient.getNumber(),
                patient.isActive(),
                details.name(),
                details.surname(),
                details.dni().value(),
                details.birthDate(),
                details.address(),
                details.phoneNumber(),
                details.sip(),
                details.visitCode(),
                details.belongings(),
                details.abuseSubstances(),
                details.extraInfo(),
                patient.getEntryDate(),
                patient.getExitDate());
    }
}
