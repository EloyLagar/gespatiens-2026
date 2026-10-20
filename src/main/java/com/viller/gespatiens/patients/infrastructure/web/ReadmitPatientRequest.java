package com.viller.gespatiens.patients.infrastructure.web;

import java.time.LocalDate;

// DTO de entrada del reingreso. El cuerpo entero es opcional: sin él, el reingreso es con fecha de hoy
public record ReadmitPatientRequest(LocalDate entryDate) {

    public static LocalDate entryDateOrToday(ReadmitPatientRequest request) {
        return request != null && request.entryDate() != null ? request.entryDate() : LocalDate.now();
    }
}
