package com.viller.gespatiens.diary.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActivityRepository {
    Activity save(Activity activity);
    Optional<Activity> findById(UUID id);
    // Por hora de inicio
    List<Activity> findByDate(LocalDate date);
    // Las de varios días (ambos incluidos), por día y hora: la tabla mensual de valoraciones
    List<Activity> findBetween(LocalDate from, LocalDate to);
    void delete(Activity activity);
}
