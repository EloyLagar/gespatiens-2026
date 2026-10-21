package com.viller.gespatiens.diary.application;

import com.viller.gespatiens.diary.application.DiaryDayView.OutingView;
import com.viller.gespatiens.diary.domain.DiaryEntryNotFoundException;
import com.viller.gespatiens.diary.domain.ResidentDirectory;
import com.viller.gespatiens.diary.domain.TherapeuticOuting;
import com.viller.gespatiens.diary.domain.TherapeuticOutingRepository;

import java.time.LocalTime;
import java.util.UUID;

// Anotar el regreso de una salida terapéutica, o corregir la hora de salida o la nota
public class UpdateOutingUseCase {

    private final TherapeuticOutingRepository outingRepository;
    private final ResidentDirectory residentDirectory;

    public UpdateOutingUseCase(TherapeuticOutingRepository outingRepository, ResidentDirectory residentDirectory) {
        this.outingRepository = outingRepository;
        this.residentDirectory = residentDirectory;
    }

    public OutingView execute(UUID outingId, LocalTime leftAt, LocalTime returnedAt, String notes) {
        TherapeuticOuting outing = outingRepository.findById(outingId)
                .orElseThrow(() -> new DiaryEntryNotFoundException("salida", outingId));
        outing.update(leftAt, returnedAt, notes);
        TherapeuticOuting saved = outingRepository.save(outing);
        return DiaryViews.outingView(saved, residentDirectory)
                .orElseThrow(() -> new DiaryEntryNotFoundException("salida", outingId));
    }
}
