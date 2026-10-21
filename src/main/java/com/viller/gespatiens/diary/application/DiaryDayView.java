package com.viller.gespatiens.diary.application;

import com.viller.gespatiens.diary.domain.ActivityType;
import com.viller.gespatiens.diary.domain.AttendanceStatus;
import com.viller.gespatiens.diary.domain.DayPart;
import com.viller.gespatiens.diary.domain.Evaluation;
import com.viller.gespatiens.diary.domain.Resident;
import com.viller.gespatiens.diary.domain.StaffMember;

import java.math.BigDecimal;
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

    // updatedAt vacío: nadie ha rellenado todavía ese turno. version: la que hay que mandar al guardarlo (RF8)
    public record ShiftView(DayPart part, List<StaffMember> staff, Optional<String> notes,
                            Optional<StaffMember> updatedBy, Optional<Instant> updatedAt, long version) {
    }

    public record ReductionView(Resident resident, Optional<String> partial, boolean total, boolean sport) {
    }

    // version: la que hay que mandar al guardar la actividad, su lista o sus valoraciones (RF8)
    public record ActivityView(UUID id, LocalTime startTime, ActivityType type, Optional<String> title,
                               List<AttendanceRow> attendance, EvaluationAverages averages, long version) {
    }

    // Un residente en la lista de una actividad. reduced: su rebaja le libra de esta actividad (se muestra
    // "Rebajado" y no se le pasa lista). partialReduction: su rebaja parcial, para que el personal la tenga en cuenta.
    // evaluation: su valoración, solo si asistió
    public record AttendanceRow(Resident resident, Optional<AttendanceStatus> status, boolean reduced,
                                Optional<String> partialReduction, Optional<Evaluation> evaluation) {
    }

    // Las medias de las valoraciones de una actividad, con un decimal. Vacía: nadie tiene ese dato
    public record EvaluationAverages(Optional<BigDecimal> mark, Optional<BigDecimal> satisfaction,
                                     Optional<BigDecimal> utility) {
    }

    public record OutingView(UUID id, Resident resident, LocalTime leftAt, Optional<LocalTime> returnedAt,
                             Optional<String> notes) {
    }
}
