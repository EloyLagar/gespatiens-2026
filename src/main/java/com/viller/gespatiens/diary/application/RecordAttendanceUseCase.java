package com.viller.gespatiens.diary.application;

import com.viller.gespatiens.diary.application.DiaryDayView.ActivityView;
import com.viller.gespatiens.diary.domain.Activity;
import com.viller.gespatiens.diary.domain.ActivityRepository;
import com.viller.gespatiens.diary.domain.AttendanceStatus;
import com.viller.gespatiens.diary.domain.DiaryEntryNotFoundException;
import com.viller.gespatiens.diary.domain.Reduction;
import com.viller.gespatiens.diary.domain.ReductionRepository;
import com.viller.gespatiens.diary.domain.ResidentDirectory;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Pasar lista de una actividad. A quien su rebaja libra de ella no se le anota nada: figura como "Rebajado"
public class RecordAttendanceUseCase {

    private final ActivityRepository activityRepository;
    private final ReductionRepository reductionRepository;
    private final ResidentDirectory residentDirectory;
    private final Clock clock;

    public RecordAttendanceUseCase(ActivityRepository activityRepository, ReductionRepository reductionRepository,
                                   ResidentDirectory residentDirectory, Clock clock) {
        this.activityRepository = activityRepository;
        this.reductionRepository = reductionRepository;
        this.residentDirectory = residentDirectory;
        this.clock = clock;
    }

    public ActivityView execute(UUID activityId, Map<UUID, AttendanceStatus> entries) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new DiaryEntryNotFoundException("actividad", activityId));
        Map<UUID, Reduction> reductions = DiaryViews.reductionsByPatient(reductionRepository, activity);
        Map<UUID, AttendanceStatus> kept = new HashMap<>();
        (entries == null ? Map.<UUID, AttendanceStatus>of() : entries).forEach((patientId, status) -> {
            if (status == null) {
                return; // Sin anotar todavía
            }
            if (residentDirectory.find(patientId).isEmpty()) {
                throw new IllegalArgumentException("No existe ningún residente con id " + patientId);
            }
            Reduction reduction = reductions.get(patientId);
            if (reduction == null || !reduction.excuses(activity.getType())) {
                kept.put(patientId, status);
            }
        });
        activity.recordAttendance(kept, LocalDate.now(clock));
        return DiaryViews.activityView(activityRepository.save(activity), residentDirectory, reductions);
    }
}
