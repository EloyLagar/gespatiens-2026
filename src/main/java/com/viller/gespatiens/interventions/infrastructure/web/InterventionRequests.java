package com.viller.gespatiens.interventions.infrastructure.web;

import java.time.LocalDate;
import java.util.UUID;

// DTO de entrada de las intervenciones
public final class InterventionRequests {

    private InterventionRequests() {
    }

    // Los dos tutores del residente: un educador social y un psicólogo. null: ese queda sin asignar
    public record TutorsRequest(UUID educatorId, UUID psychologistId) {
    }

    // Sirve para registrar y para corregir una intervención
    public record InterventionRequest(LocalDate date, String text) {
    }
}
