package com.viller.gespatiens.diary.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TherapeuticOutingRepository {
    TherapeuticOuting save(TherapeuticOuting outing);
    Optional<TherapeuticOuting> findById(UUID id);
    // Por hora de salida
    List<TherapeuticOuting> findByDate(LocalDate date);
    void delete(TherapeuticOuting outing);
}
