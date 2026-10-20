package com.viller.gespatiens.diary.application;

import com.viller.gespatiens.diary.domain.DiaryEntryNotFoundException;
import com.viller.gespatiens.diary.domain.TherapeuticOutingRepository;

import java.util.UUID;

// Quitar una salida terapéutica anotada por error
public class DeleteOutingUseCase {

    private final TherapeuticOutingRepository outingRepository;

    public DeleteOutingUseCase(TherapeuticOutingRepository outingRepository) {
        this.outingRepository = outingRepository;
    }

    public void execute(UUID outingId) {
        outingRepository.delete(outingRepository.findById(outingId)
                .orElseThrow(() -> new DiaryEntryNotFoundException("salida", outingId)));
    }
}
