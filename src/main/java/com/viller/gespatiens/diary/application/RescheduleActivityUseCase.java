package com.viller.gespatiens.diary.application;

import com.viller.gespatiens.diary.application.DiaryDayView.ActivityView;
import com.viller.gespatiens.diary.domain.Activity;
import com.viller.gespatiens.diary.domain.ActivityRepository;
import com.viller.gespatiens.diary.domain.ActivityType;
import com.viller.gespatiens.diary.domain.DiaryEntryNotFoundException;
import com.viller.gespatiens.diary.domain.ReductionRepository;
import com.viller.gespatiens.diary.domain.ResidentDirectory;

import java.time.LocalTime;
import java.util.UUID;

// Cambiar la hora, el tipo o el nombre de una actividad. La lista que ya se pasó se conserva
public class RescheduleActivityUseCase {

    private final ActivityRepository activityRepository;
    private final ReductionRepository reductionRepository;
    private final ResidentDirectory residentDirectory;

    public RescheduleActivityUseCase(ActivityRepository activityRepository, ReductionRepository reductionRepository,
                                     ResidentDirectory residentDirectory) {
        this.activityRepository = activityRepository;
        this.reductionRepository = reductionRepository;
        this.residentDirectory = residentDirectory;
    }

    public ActivityView execute(UUID activityId, LocalTime startTime, ActivityType type, String title) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new DiaryEntryNotFoundException("actividad", activityId));
        activity.reschedule(startTime, type, title);
        Activity saved = activityRepository.save(activity);
        return DiaryViews.activityView(saved, residentDirectory,
                DiaryViews.reductionsByPatient(reductionRepository, saved));
    }
}
