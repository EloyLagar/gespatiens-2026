package com.viller.gespatiens.patients.infrastructure.web;

import com.viller.gespatiens.patients.domain.Dni;
import com.viller.gespatiens.patients.domain.PatientDetails;

import java.time.LocalDate;

// DTO de entrada: lo que llega en el JSON del POST. Las fechas en formato yyyy-MM-dd
public record AdmitPatientRequest(
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
        // Opcional: si no se indica, el alta es con fecha de hoy
        LocalDate entryDate) {

    public PatientDetails toDetails() {
        return new PatientDetails(name, surname, new Dni(dni), birthDate, address, phoneNumber,
                sip, visitCode, belongings, abuseSubstances, extraInfo);
    }

    public LocalDate entryDateOrToday() {
        return entryDate != null ? entryDate : LocalDate.now();
    }
}
