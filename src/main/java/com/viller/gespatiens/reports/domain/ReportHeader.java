package com.viller.gespatiens.reports.domain;

import java.time.LocalDate;
import java.time.Period;

// La cabecera del informe. Nombre, nacimiento, DNI y S.I.P. salen siempre de la ficha. Las fechas de la
// estancia también: la de entrada es la de la estancia del informe y la de salida se apunta sola al dar de
// baja al residente. Lo que se escribe a mano es el nº de solicitud, el centro de referencia, el N.I.P.
// (solo media estancia) y el motivo de salida (solo final).
// dueDate (solo media estancia): cuándo toca el informe. Es a mitad del programa: por defecto, a los
// MID_STAY_AFTER de la entrada, y se puede cambiar (decisión del centro). El panel avisa con ella
public record ReportHeader(
        String requestNumber,
        String referenceCenter,
        String nip,
        LocalDate entryDate,
        LocalDate exitDate,
        String exitReason,
        LocalDate dueDate) {

    // El centro de referencia que sale por defecto (decisión del centro). Se puede cambiar
    public static final String DEFAULT_REFERENCE_CENTER = "Comunidad Terapéutica Los Vientos";
    // La mitad de un programa de unos seis meses
    public static final Period MID_STAY_AFTER = Period.ofMonths(3);

    public ReportHeader {
        requestNumber = optional(requestNumber, "nº de solicitud", 30);
        referenceCenter = optional(referenceCenter, "centro de referencia", 150);
        nip = optional(nip, "N.I.P.", 30);
        exitReason = optional(exitReason, "motivo de salida", 200);
        if (entryDate != null && exitDate != null && exitDate.isBefore(entryDate)) {
            throw new IllegalArgumentException("La fecha de salida no puede ser anterior a la de entrada");
        }
        if (entryDate != null && dueDate != null && dueDate.isBefore(entryDate)) {
            throw new IllegalArgumentException("El informe no puede tocar antes de la fecha de entrada");
        }
    }

    // La de un informe recién creado al empezar una estancia
    public static ReportHeader forStay(LocalDate entryDate) {
        return new ReportHeader(null, DEFAULT_REFERENCE_CENTER, null, entryDate, null, null,
                defaultDueDate(entryDate));
    }

    private static LocalDate defaultDueDate(LocalDate entryDate) {
        return entryDate == null ? null : entryDate.plus(MID_STAY_AFTER);
    }

    // Lo que se escribe a mano. Las fechas de la estancia no se tocan. Sin fecha prevista vuelve la de por defecto
    public ReportHeader withEditable(String requestNumber, String referenceCenter, String nip, String exitReason,
                                     LocalDate dueDate) {
        return new ReportHeader(requestNumber, referenceCenter, nip, entryDate, exitDate, exitReason,
                dueDate == null ? defaultDueDate(entryDate) : dueDate);
    }

    ReportHeader withExitDate(LocalDate exitDate) {
        return new ReportHeader(requestNumber, referenceCenter, nip, entryDate, exitDate, exitReason, dueDate);
    }

    // Lo que no corresponde al tipo de informe no se guarda: así un cambio de formulario no deja datos ocultos
    ReportHeader forType(ReportType type) {
        return type == ReportType.MID_STAY
                ? new ReportHeader(requestNumber, referenceCenter, nip, entryDate, exitDate, null,
                        dueDate == null ? defaultDueDate(entryDate) : dueDate)
                : new ReportHeader(requestNumber, referenceCenter, null, entryDate, exitDate, exitReason, null);
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
