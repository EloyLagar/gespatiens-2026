package com.viller.gespatiens.diary.application;

import com.viller.gespatiens.diary.domain.Reduction;
import com.viller.gespatiens.diary.domain.ReductionNotFoundException;
import com.viller.gespatiens.diary.domain.ReductionRepository;

import java.time.LocalDate;
import java.util.UUID;

// Quitar una rebaja registrada por error. Como registrarla: equipo médico y administración (SecurityConfig)
public class DeleteReductionUseCase {

    private final ReductionRepository reductionRepository;

    public DeleteReductionUseCase(ReductionRepository reductionRepository) {
        this.reductionRepository = reductionRepository;
    }

    public void execute(LocalDate date, UUID patientId) {
        Reduction reduction = reductionRepository.findByDateAndPatient(date, patientId)
                .orElseThrow(() -> new ReductionNotFoundException(date, patientId));
        reductionRepository.delete(reduction);
    }
}
