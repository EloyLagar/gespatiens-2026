package com.viller.gespatiens.interventions.infrastructure.web;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

// DTO de entrada de las intervenciones
public final class InterventionRequests {

    private InterventionRequests() {
    }

    // Sustituye a todos los tutores. Vacío: el residente se queda sin tutores
    public record TutorsRequest(Set<UUID> tutorIds) {
    }

    // Sirve para registrar y para corregir una intervención
    public record InterventionRequest(LocalDate date, String text) {
    }
}
