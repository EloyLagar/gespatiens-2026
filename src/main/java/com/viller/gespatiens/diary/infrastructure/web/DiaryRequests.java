package com.viller.gespatiens.diary.infrastructure.web;

import com.viller.gespatiens.diary.domain.ActivityType;
import com.viller.gespatiens.diary.domain.AttendanceStatus;

import java.time.LocalTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// DTO de entrada del diario
public final class DiaryRequests {

    private DiaryRequests() {
    }

    public record ShiftRequest(Set<UUID> staffIds, String notes) {
    }

    public record ReductionRequest(String partial, boolean total, boolean sport) {
    }

    // Sirve para programar y para cambiar una actividad
    public record ActivityRequest(LocalTime startTime, ActivityType type, String title) {
    }

    // Residente -> lo anotado. Quien no aparece (o va a null) queda sin anotar
    public record AttendanceRequest(Map<UUID, AttendanceStatus> attendance) {
    }

    public record OutingRequest(UUID patientId, LocalTime leftAt, String notes) {
    }

    // returnedAt null: todavía no ha vuelto
    public record OutingUpdateRequest(LocalTime leftAt, LocalTime returnedAt, String notes) {
    }
}
