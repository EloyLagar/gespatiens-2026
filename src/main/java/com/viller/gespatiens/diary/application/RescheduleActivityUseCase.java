package com.viller.gespatiens.diary.application;

import com.viller.gespatiens.diary.application.DiaryDayView.ActivityView;
import com.viller.gespatiens.diary.domain.Activity;
import com.viller.gespatiens.diary.domain.ActivityRepository;
import com.viller.gespatiens.diary.domain.ActivityType;
import com.viller.gespatiens.diary.domain.DiaryEntryNotFoundException;
import com.viller.gespatiens.diary.domain.EditingPresence;
import com.viller.gespatiens.diary.domain.ReductionRepository;
import com.viller.gespatiens.diary.domain.ResidentDirectory;

import java.time.LocalTime;
import java.util.UUID;

// Cambiar la hora, el tipo o el nombre de una actividad. La lista que ya se pasó se conserva
public class RescheduleActivityUseCase {

    private final ActivityRepository activityRepository;
    private final ReductionRepository reductionRepository;
    private final ResidentDirectory residentDirectory;
    private final EditingPresence editingPresence;

    public RescheduleActivityUseCase(ActivityRepository activityRepository, ReductionRepository reductionRepository,
                                     ResidentDirectory residentDirectory, EditingPresence editingPresence) {
        this.activityRepository = activityRepository;
        this.reductionRepository = reductionRepository;
        this.residentDirectory = residentDirectory;
        this.editingPresence = editingPresence;
    }

    // RF8: expectedVersion es la que tenía la actividad en pantalla; null no lo comprueba
    public ActivityView execute(UUID activityId, LocalTime startTime, ActivityType type, String title, UUID editorId,
                                Long expectedVersion) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new DiaryEntryNotFoundException("actividad", activityId));
        editingPresence.ensureNotEditedByOther(EditingPresence.activity(activityId), editorId);
        activity.checkVersion(expectedVersion);
        activity.reschedule(startTime, type, title);
        Activity saved = activityRepository.save(activity);
        return DiaryViews.activityView(saved, residentDirectory,
                DiaryViews.reductionsByPatient(reductionRepository, saved));
    }
}
