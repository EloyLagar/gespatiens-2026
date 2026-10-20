package com.viller.gespatiens.diary.application;

import com.viller.gespatiens.diary.application.DiaryDayView.OutingView;
import com.viller.gespatiens.diary.domain.Resident;
import com.viller.gespatiens.diary.domain.ResidentDirectory;
import com.viller.gespatiens.diary.domain.TherapeuticOuting;
import com.viller.gespatiens.diary.domain.TherapeuticOutingRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

// Anotar que un residente sale del centro en una salida terapéutica. Cualquier empleado
public class RecordOutingUseCase {

    private final TherapeuticOutingRepository outingRepository;
    private final ResidentDirectory residentDirectory;

    public RecordOutingUseCase(TherapeuticOutingRepository outingRepository, ResidentDirectory residentDirectory) {
        this.outingRepository = outingRepository;
        this.residentDirectory = residentDirectory;
    }

    public OutingView execute(LocalDate date, UUID patientId, LocalTime leftAt, String notes) {
        if (date == null) {
            throw new IllegalArgumentException("Indica el día de la salida");
        }
        Resident resident = residentDirectory.find(patientId)
                .orElseThrow(() -> new IllegalArgumentException("No existe ningún residente con id " + patientId));
        if (!resident.active()) {
            throw new IllegalArgumentException("Solo pueden salir residentes que están en el centro");
        }
        TherapeuticOuting outing = outingRepository.save(TherapeuticOuting.leave(date, patientId, leftAt, notes));
        return new OutingView(outing.getId(), resident, outing.getLeftAt(), outing.getReturnedAt(), outing.getNotes());
    }
}
