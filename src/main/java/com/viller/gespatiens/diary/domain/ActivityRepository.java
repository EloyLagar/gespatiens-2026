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
    void delete(Activity activity);
}
