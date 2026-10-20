package com.viller.gespatiens.patients.infrastructure.web;

import java.time.LocalDate;

// DTO de entrada de la baja. El cuerpo entero es opcional: sin él, la baja es con fecha de hoy
public record DischargePatientRequest(LocalDate exitDate) {

    public static LocalDate exitDateOrToday(DischargePatientRequest request) {
        return request != null && request.exitDate() != null ? request.exitDate() : LocalDate.now();
    }
}
