package com.viller.gespatiens.diary.application;

import com.viller.gespatiens.diary.application.DiaryDayView.ReductionView;
import com.viller.gespatiens.diary.domain.Reduction;
import com.viller.gespatiens.diary.domain.ReductionRepository;
import com.viller.gespatiens.diary.domain.Resident;
import com.viller.gespatiens.diary.domain.ResidentDirectory;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

// La memoria: "existirá un espacio para que el equipo médico pueda registrar los pacientes rebajados
// con su tipo de rebaja". Quién puede (equipo médico y administración) lo comprueba SecurityConfig.
// Registrar la rebaja de un residente que ya la tenía ese día la sustituye
public class RecordReductionUseCase {

    private final ReductionRepository reductionRepository;
    private final ResidentDirectory residentDirectory;
    private final Clock clock;

    public RecordReductionUseCase(ReductionRepository reductionRepository, ResidentDirectory residentDirectory,
                                  Clock clock) {
        this.reductionRepository = reductionRepository;
        this.residentDirectory = residentDirectory;
        this.clock = clock;
    }

    public ReductionView execute(LocalDate date, UUID patientId, String partial, boolean total, boolean sport,
                                 UUID recorderId) {
        if (date == null) {
            throw new IllegalArgumentException("Indica el día de la rebaja");
        }
        Resident resident = residentDirectory.find(patientId)
                .orElseThrow(() -> new IllegalArgumentException("No existe ningún residente con id " + patientId));
        if (!resident.active()) {
            throw new IllegalArgumentException("Solo se puede rebajar a residentes que están en el centro");
        }
        Reduction reduction = reductionRepository.findByDateAndPatient(date, patientId)
                .map(existing -> {
                    existing.change(partial, total, sport, recorderId, clock.instant());
                    return existing;
                })
                .orElseGet(() -> Reduction.record(date, patientId, partial, total, sport, recorderId, clock.instant()));
        Reduction saved = reductionRepository.save(reduction);
        return new ReductionView(resident, saved.getPartial(), saved.isTotal(), saved.isSport());
    }
}
