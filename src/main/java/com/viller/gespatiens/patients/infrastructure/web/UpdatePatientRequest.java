package com.viller.gespatiens.patients.infrastructure.web;

import com.viller.gespatiens.patients.domain.Dni;
import com.viller.gespatiens.patients.domain.PatientDetails;

import java.time.LocalDate;

// DTO de entrada del PUT: la ficha completa. Un campo opcional que no se envía queda vacío.
// El número y las fechas de entrada y salida no se editan aquí (alta, baja y reingreso)
public record UpdatePatientRequest(
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
        String extraInfo) {

    public PatientDetails toDetails() {
        return new PatientDetails(name, surname, new Dni(dni), birthDate, address, phoneNumber,
                sip, visitCode, belongings, abuseSubstances, extraInfo);
    }
}
