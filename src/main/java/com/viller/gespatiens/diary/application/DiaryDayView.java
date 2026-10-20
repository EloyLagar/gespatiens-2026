package com.viller.gespatiens.diary.application;

import com.viller.gespatiens.diary.domain.ActivityType;
import com.viller.gespatiens.diary.domain.AttendanceStatus;
import com.viller.gespatiens.diary.domain.DayPart;
import com.viller.gespatiens.diary.domain.Resident;
import com.viller.gespatiens.diary.domain.StaffMember;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Una página del diario: los tres turnos (rellenados o no), los residentes rebajados, las actividades
// con su asistencia y las salidas terapéuticas de ese día
public record DiaryDayView(LocalDate date, List<ShiftView> shifts, List<ReductionView> reductions,
                           List<ActivityView> activities, List<OutingView> outings) {

    // updatedAt vacío: nadie ha rellenado todavía ese turno
    public record ShiftView(DayPart part, List<StaffMember> staff, Optional<String> notes,
                            Optional<StaffMember> updatedBy, Optional<Instant> updatedAt) {
    }

    public record ReductionView(Resident resident, Optional<String> partial, boolean total, boolean sport) {
    }

    public record ActivityView(UUID id, LocalTime startTime, ActivityType type, Optional<String> title,
                               List<AttendanceRow> attendance) {
    }

    // Un residente en la lista de una actividad. reduced: su rebaja le libra de esta actividad (se muestra
    // "Rebajado" y no se le pasa lista). partialReduction: su rebaja parcial, para que el personal la tenga en cuenta
    public record AttendanceRow(Resident resident, Optional<AttendanceStatus> status, boolean reduced,
                                Optional<String> partialReduction) {
    }

    public record OutingView(UUID id, Resident resident, LocalTime leftAt, Optional<LocalTime> returnedAt,
                             Optional<String> notes) {
    }
}
