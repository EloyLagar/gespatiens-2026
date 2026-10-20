package com.viller.gespatiens.diary.application;

import com.viller.gespatiens.diary.application.DiaryDayView.ActivityView;
import com.viller.gespatiens.diary.domain.Activity;
import com.viller.gespatiens.diary.domain.ActivityRepository;
import com.viller.gespatiens.diary.domain.DiaryEntryNotFoundException;
import com.viller.gespatiens.diary.domain.Evaluation;
import com.viller.gespatiens.diary.domain.Reduction;
import com.viller.gespatiens.diary.domain.ReductionRepository;
import com.viller.gespatiens.diary.domain.ResidentDirectory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// RF6: "asignar notas a los usuarios respecto a las actividades" y recoger la satisfacción y la utilidad que
// le dan. Solo a quien asistió y no está rebajado. Quién puede (educadores y administración) lo comprueba
// SecurityConfig
public class RecordEvaluationsUseCase {

    private final ActivityRepository activityRepository;
    private final ReductionRepository reductionRepository;
    private final ResidentDirectory residentDirectory;

    public RecordEvaluationsUseCase(ActivityRepository activityRepository, ReductionRepository reductionRepository,
                                    ResidentDirectory residentDirectory) {
        this.activityRepository = activityRepository;
        this.reductionRepository = reductionRepository;
        this.residentDirectory = residentDirectory;
    }

    public ActivityView execute(UUID activityId, Map<UUID, Evaluation> entries) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new DiaryEntryNotFoundException("actividad", activityId));
        Map<UUID, Reduction> reductions = DiaryViews.reductionsByPatient(reductionRepository, activity);
        Map<UUID, Evaluation> kept = new HashMap<>();
        (entries == null ? Map.<UUID, Evaluation>of() : entries).forEach((patientId, evaluation) -> {
            if (evaluation == null) {
                return; // Sin valorar
            }
            Reduction reduction = reductions.get(patientId);
            if (reduction != null && reduction.excuses(activity.getType())) {
                throw new IllegalArgumentException("No se puede valorar a un residente rebajado de la actividad");
            }
            kept.put(patientId, evaluation);
        });
        activity.recordEvaluations(kept);
        return DiaryViews.activityView(activityRepository.save(activity), residentDirectory, reductions);
    }
}
