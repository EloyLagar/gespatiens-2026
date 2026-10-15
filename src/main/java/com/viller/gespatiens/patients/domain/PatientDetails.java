package com.viller.gespatiens.patients.domain;

import java.time.LocalDate;

// Value object con los datos personales de la ficha (memoria, apartado 4.1).
// Agruparlos evita constructores de 15 parámetros en Patient y concentra sus validaciones.
public record PatientDetails(
        String name,
        String surname,
        Dni dni,
        LocalDate birthDate,
        String address,
        String phoneNumber,
        String sip,
        String visitCode,
        String belongings,
        String abuseSubstances,
        String extraInfo) {

    public PatientDetails {
        // Un campo opcional enviado vacío ("") se guarda como null: así dos residentes
        // sin código de visita no chocan con la restricción unique
        sip = blankToNull(sip);
        visitCode = blankToNull(visitCode);
        belongings = blankToNull(belongings);
        abuseSubstances = blankToNull(abuseSubstances);
        extraInfo = blankToNull(extraInfo);
        requireText(name, "nombre", 50);
        requireText(surname, "apellidos", 100);
        requireText(address, "dirección", 500);
        requireText(phoneNumber, "teléfono", 20);
        if (dni == null) {
            throw new IllegalArgumentException("El DNI no puede estar vacío");
        }
        if (birthDate == null) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede estar vacía");
        }
        if (birthDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura");
        }
        maxLength(sip, "SIP", 25);
        maxLength(visitCode, "código de visita", 25);
        maxLength(abuseSubstances, "sustancias de abuso", 100);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static void requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El campo " + field + " no puede estar vacío");
        }
        maxLength(value, field, maxLength);
    }

    private static void maxLength(String value, String field, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(
                    "El campo " + field + " no puede superar los " + maxLength + " caracteres");
        }
    }
}
