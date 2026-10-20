package com.viller.gespatiens.diary.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ShiftLogRepository {
    ShiftLog save(ShiftLog shiftLog);
    List<ShiftLog> findByDate(LocalDate date);
    Optional<ShiftLog> findByDateAndPart(LocalDate date, DayPart part);
}
