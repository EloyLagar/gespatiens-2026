package com.viller.gespatiens.diary.application;

import com.viller.gespatiens.diary.application.DiaryDayView.ShiftView;
import com.viller.gespatiens.diary.domain.DayPart;
import com.viller.gespatiens.diary.domain.DiaryConflictException;
import com.viller.gespatiens.diary.domain.EditingPresence;
import com.viller.gespatiens.diary.domain.ShiftLog;
import com.viller.gespatiens.diary.domain.ShiftLogRepository;
import com.viller.gespatiens.diary.domain.StaffDirectory;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

// Rellenar o corregir un turno: quién trabajó y la información de interés. Cualquier empleado.
// RF8: no se guarda si otra persona lo está editando o lo cambió mientras tanto
public class RecordShiftUseCase {

    private final ShiftLogRepository shiftLogRepository;
    private final StaffDirectory staffDirectory;
    private final EditingPresence editingPresence;
    private final Clock clock;

    public RecordShiftUseCase(ShiftLogRepository shiftLogRepository, StaffDirectory staffDirectory,
                              EditingPresence editingPresence, Clock clock) {
        this.shiftLogRepository = shiftLogRepository;
        this.staffDirectory = staffDirectory;
        this.editingPresence = editingPresence;
        this.clock = clock;
    }

    // expectedVersion: la que tenía el turno en pantalla (0 si estaba sin rellenar); null no lo comprueba
    public ShiftView execute(LocalDate date, DayPart part, Set<UUID> staffIds, String notes, UUID editorId,
                             Long expectedVersion) {
        if (date == null || part == null) {
            throw new IllegalArgumentException("Indica el día y el turno");
        }
        editingPresence.ensureNotEditedByOther(EditingPresence.shift(date, part), editorId);
        Set<UUID> staff = staffIds == null ? Set.of() : staffIds;
        for (UUID id : staff) {
            if (staffDirectory.find(id).isEmpty()) {
                throw new IllegalArgumentException("No existe ningún empleado con id " + id);
            }
        }
        ShiftLog log = shiftLogRepository.findByDateAndPart(date, part)
                .map(existing -> {
                    existing.checkVersion(expectedVersion);
                    existing.update(staff, notes, editorId, clock.instant());
                    return existing;
                })
                .orElseGet(() -> {
                    if (expectedVersion != null && expectedVersion != 0) {
                        throw new DiaryConflictException("el turno");
                    }
                    return ShiftLog.record(date, part, staff, notes, editorId, clock.instant());
                });
        return GetDiaryDayUseCase.shiftView(shiftLogRepository.save(log), staffDirectory);
    }
}
