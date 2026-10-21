package com.viller.gespatiens.reports.domain;

import java.time.LocalDate;
import java.util.UUID;

// Lo que los informes necesitan saber de un residente: los datos de su ficha que van en la cabecera.
// number es null si ya no está en el centro; exitDate, si sigue en él
public record Resident(UUID id, Integer number, String name, String surname, String dni, LocalDate birthDate,
                       String sip, LocalDate entryDate, LocalDate exitDate, boolean active) {
}
