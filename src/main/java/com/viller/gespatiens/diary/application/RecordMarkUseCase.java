package com.viller.gespatiens.diary.application;

import com.viller.gespatiens.diary.domain.Activity;
import com.viller.gespatiens.diary.domain.ActivityRepository;
import com.viller.gespatiens.diary.domain.DiaryEntryNotFoundException;
import com.viller.gespatiens.diary.domain.EditingPresence;
import com.viller.gespatiens.diary.domain.Reduction;
import com.viller.gespatiens.diary.domain.ReductionRepository;

import java.math.BigDecimal;
import java.util.UUID;

// La nota de un residente en una actividad, desde la tabla mensual (RF6). Quién puede (educadores y
// administración) lo comprueba SecurityConfig. RF8: no se guarda si otra persona tiene la actividad abierta
// o la cambió mientras tanto. Devuelve la versión nueva de la actividad, para el siguiente cambio
public class RecordMarkUseCase {

    private final ActivityRepository activityRepository;
    private final ReductionRepository reductionRepository;
    private final EditingPresence editingPresence;

    public RecordMarkUseCase(ActivityRepository activityRepository, ReductionRepository reductionRepository,
                             EditingPresence editingPresence) {
        this.activityRepository = activityRepository;
        this.reductionRepository = reductionRepository;
        this.editingPresence = editingPresence;
    }

    // mark null: quita la nota
    public long execute(UUID activityId, UUID patientId, BigDecimal mark, UUID editorId, Long expectedVersion) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new DiaryEntryNotFoundException("actividad", activityId));
        editingPresence.ensureNotEditedByOther(EditingPresence.activity(activityId), editorId);
        activity.checkVersion(expectedVersion);
        Reduction reduction = DiaryViews.reductionsByPatient(reductionRepository, activity).get(patientId);
        if (reduction != null && reduction.excuses(activity.getType())) {
            throw new IllegalArgumentException("No se puede valorar a un residente rebajado de la actividad");
        }
        activity.recordMark(patientId, mark);
        return activityRepository.save(activity).version();
    }
}
