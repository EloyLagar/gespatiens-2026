package com.viller.gespatiens.diary.application;

import com.viller.gespatiens.diary.application.DiaryDayView.ActivityView;
import com.viller.gespatiens.diary.domain.Activity;
import com.viller.gespatiens.diary.domain.ActivityRepository;
import com.viller.gespatiens.diary.domain.ActivityType;
import com.viller.gespatiens.diary.domain.ReductionRepository;
import com.viller.gespatiens.diary.domain.ResidentDirectory;

import java.time.LocalDate;
import java.time.LocalTime;

// Programar una actividad en el diario (también con antelación). Cualquier empleado
public class ScheduleActivityUseCase {

    private final ActivityRepository activityRepository;
    private final ReductionRepository reductionRepository;
    private final ResidentDirectory residentDirectory;

    public ScheduleActivityUseCase(ActivityRepository activityRepository, ReductionRepository reductionRepository,
                                   ResidentDirectory residentDirectory) {
        this.activityRepository = activityRepository;
        this.reductionRepository = reductionRepository;
        this.residentDirectory = residentDirectory;
    }

    public ActivityView execute(LocalDate date, LocalTime startTime, ActivityType type, String title) {
        if (date == null) {
            throw new IllegalArgumentException("Indica el día de la actividad");
        }
        Activity activity = activityRepository.save(Activity.schedule(date, startTime, type, title));
        return DiaryViews.activityView(activity, residentDirectory,
                DiaryViews.reductionsByPatient(reductionRepository, activity));
    }
}
