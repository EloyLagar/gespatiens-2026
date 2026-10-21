package com.viller.gespatiens.diary.application;

import com.viller.gespatiens.diary.domain.Activity;
import com.viller.gespatiens.diary.domain.ActivityRepository;
import com.viller.gespatiens.diary.domain.DiaryEntryNotFoundException;
import com.viller.gespatiens.diary.domain.EditingPresence;

import java.util.UUID;

// Quitar una actividad del diario, con su lista de asistencia. No mientras otra persona la está editando
public class DeleteActivityUseCase {

    private final ActivityRepository activityRepository;
    private final EditingPresence editingPresence;

    public DeleteActivityUseCase(ActivityRepository activityRepository, EditingPresence editingPresence) {
        this.activityRepository = activityRepository;
        this.editingPresence = editingPresence;
    }

    public void execute(UUID activityId, UUID editorId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new DiaryEntryNotFoundException("actividad", activityId));
        editingPresence.ensureNotEditedByOther(EditingPresence.activity(activityId), editorId);
        activityRepository.delete(activity);
    }
}
