package com.viller.gespatiens.diary.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReductionRepository {
    Reduction save(Reduction reduction);
    List<Reduction> findByDate(LocalDate date);
    Optional<Reduction> findByDateAndPatient(LocalDate date, UUID patientId);
    void delete(Reduction reduction);
}
