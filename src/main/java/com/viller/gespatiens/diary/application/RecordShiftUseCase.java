package com.viller.gespatiens.diary.application;

import com.viller.gespatiens.diary.application.DiaryDayView.ShiftView;
import com.viller.gespatiens.diary.domain.DayPart;
import com.viller.gespatiens.diary.domain.ShiftLog;
import com.viller.gespatiens.diary.domain.ShiftLogRepository;
import com.viller.gespatiens.diary.domain.StaffDirectory;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

// Rellenar o corregir un turno: quién trabajó y la información de interés. Cualquier empleado
public class RecordShiftUseCase {

    private final ShiftLogRepository shiftLogRepository;
    private final StaffDirectory staffDirectory;
    private final Clock clock;

    public RecordShiftUseCase(ShiftLogRepository shiftLogRepository, StaffDirectory staffDirectory, Clock clock) {
        this.shiftLogRepository = shiftLogRepository;
        this.staffDirectory = staffDirectory;
        this.clock = clock;
    }

    public ShiftView execute(LocalDate date, DayPart part, Set<UUID> staffIds, String notes, UUID editorId) {
        if (date == null || part == null) {
            throw new IllegalArgumentException("Indica el día y el turno");
        }
        Set<UUID> staff = staffIds == null ? Set.of() : staffIds;
        for (UUID id : staff) {
            if (staffDirectory.find(id).isEmpty()) {
                throw new IllegalArgumentException("No existe ningún empleado con id " + id);
            }
        }
        ShiftLog log = shiftLogRepository.findByDateAndPart(date, part)
                .map(existing -> {
                    existing.update(staff, notes, editorId, clock.instant());
                    return existing;
                })
                .orElseGet(() -> ShiftLog.record(date, part, staff, notes, editorId, clock.instant()));
        return GetDiaryDayUseCase.shiftView(shiftLogRepository.save(log), staffDirectory);
    }
}
