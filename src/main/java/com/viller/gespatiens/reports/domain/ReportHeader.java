package com.viller.gespatiens.reports.domain;

import java.time.LocalDate;

// La cabecera del informe. Nombre, nacimiento, DNI y S.I.P. salen siempre de la ficha. Las fechas de la
// estancia también: la de entrada es la de la estancia del informe y la de salida se apunta sola al dar de
// baja al residente. Lo que se escribe a mano es el nº de solicitud, el centro de referencia, el N.I.P.
// (solo media estancia) y el motivo de salida (solo final)
public record ReportHeader(
        String requestNumber,
        String referenceCenter,
        String nip,
        LocalDate entryDate,
        LocalDate exitDate,
        String exitReason) {

    // El centro de referencia que sale por defecto (decisión del centro). Se puede cambiar
    public static final String DEFAULT_REFERENCE_CENTER = "Comunidad Terapéutica Los Vientos";

    public ReportHeader {
        requestNumber = optional(requestNumber, "nº de solicitud", 30);
        referenceCenter = optional(referenceCenter, "centro de referencia", 150);
        nip = optional(nip, "N.I.P.", 30);
        exitReason = optional(exitReason, "motivo de salida", 200);
        if (entryDate != null && exitDate != null && exitDate.isBefore(entryDate)) {
            throw new IllegalArgumentException("La fecha de salida no puede ser anterior a la de entrada");
        }
    }

    // La de un informe recién creado al empezar una estancia
    public static ReportHeader forStay(LocalDate entryDate) {
        return new ReportHeader(null, DEFAULT_REFERENCE_CENTER, null, entryDate, null, null);
    }

    // Lo que se escribe a mano. Las fechas de la estancia no se tocan
    public ReportHeader withEditable(String requestNumber, String referenceCenter, String nip, String exitReason) {
        return new ReportHeader(requestNumber, referenceCenter, nip, entryDate, exitDate, exitReason);
    }

    ReportHeader withExitDate(LocalDate exitDate) {
        return new ReportHeader(requestNumber, referenceCenter, nip, entryDate, exitDate, exitReason);
    }

    // Lo que no corresponde al tipo de informe no se guarda: así un cambio de formulario no deja datos ocultos
    ReportHeader forType(ReportType type) {
        return type == ReportType.MID_STAY
                ? new ReportHeader(requestNumber, referenceCenter, nip, entryDate, exitDate, null)
                : new ReportHeader(requestNumber, referenceCenter, null, entryDate, exitDate, exitReason);
    }

    private static String optional(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.strip();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException("El campo " + field + " no puede superar los " + maxLength
                    + " caracteres");
        }
        return trimmed;
    }
}
