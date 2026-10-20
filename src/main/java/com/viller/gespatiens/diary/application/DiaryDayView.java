package com.viller.gespatiens.diary.application;

import com.viller.gespatiens.diary.domain.DayPart;
import com.viller.gespatiens.diary.domain.Resident;
import com.viller.gespatiens.diary.domain.StaffMember;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// Una página del diario: los tres turnos (rellenados o no) y los residentes rebajados ese día
public record DiaryDayView(LocalDate date, List<ShiftView> shifts, List<ReductionView> reductions) {

    // updatedAt vacío: nadie ha rellenado todavía ese turno
    public record ShiftView(DayPart part, List<StaffMember> staff, Optional<String> notes,
                            Optional<StaffMember> updatedBy, Optional<Instant> updatedAt) {
    }

    public record ReductionView(Resident resident, Optional<String> partial, boolean total, boolean sport) {
    }
}
