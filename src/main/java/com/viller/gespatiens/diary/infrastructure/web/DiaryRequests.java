package com.viller.gespatiens.diary.infrastructure.web;

import com.viller.gespatiens.diary.domain.ActivityType;
import com.viller.gespatiens.diary.domain.AttendanceStatus;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// DTO de entrada del diario
public final class DiaryRequests {

    private DiaryRequests() {
    }

    // RF8: al cambiar algo que ya existe, el frontend manda siempre la versión que tenía en pantalla. Sin ella
    // la comprobación se saltaba y se podía pisar lo que otra persona acababa de guardar
    static long requiredVersion(Long version) {
        if (version == null) {
            throw new IllegalArgumentException("Falta la versión de lo que tenías en pantalla. Recarga la página");
        }
        return version;
    }

    // version: la que tenía el turno en pantalla (0 si estaba sin rellenar). RF8: si no coincide, 409
    public record ShiftRequest(Set<UUID> staffIds, String notes, Long version) {
    }

    public record ReductionRequest(String partial, boolean total, boolean sport) {
    }

    // Sirve para programar y para cambiar una actividad. version: solo al cambiarla
    public record ActivityRequest(LocalTime startTime, ActivityType type, String title, Long version) {
    }

    // Residente -> lo anotado. Quien no aparece (o va a null) queda sin anotar
    public record AttendanceRequest(Map<UUID, AttendanceStatus> attendance, Long version) {
    }

    // Residente -> su valoración. Quien no aparece (o va a null, o con todo a null) queda sin valorar
    public record EvaluationsRequest(Map<UUID, EvaluationRequest> evaluations, Long version) {
    }

    // De 0 a 10 con un decimal; lo que no se tenga, a null
    public record EvaluationRequest(BigDecimal mark, BigDecimal satisfaction, BigDecimal utility) {
    }

    public record OutingRequest(UUID patientId, LocalTime leftAt, String notes) {
    }

    // returnedAt null: todavía no ha vuelto
    public record OutingUpdateRequest(LocalTime leftAt, LocalTime returnedAt, String notes) {
    }
}
