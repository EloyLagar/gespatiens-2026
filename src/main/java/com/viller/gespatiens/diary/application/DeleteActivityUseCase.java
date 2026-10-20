package com.viller.gespatiens.diary.application;

import com.viller.gespatiens.diary.domain.ActivityRepository;
import com.viller.gespatiens.diary.domain.DiaryEntryNotFoundException;

import java.util.UUID;

// Quitar una actividad del diario, con su lista de asistencia
public class DeleteActivityUseCase {

    private final ActivityRepository activityRepository;

    public DeleteActivityUseCase(ActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    public void execute(UUID activityId) {
        activityRepository.delete(activityRepository.findById(activityId)
                .orElseThrow(() -> new DiaryEntryNotFoundException("actividad", activityId)));
    }
}
