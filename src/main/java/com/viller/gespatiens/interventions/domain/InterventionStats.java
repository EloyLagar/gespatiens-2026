package com.viller.gespatiens.interventions.domain;

import java.time.LocalDate;

// Resumen de las intervenciones de un residente para el listado
public record InterventionStats(long count, LocalDate last) {
}
