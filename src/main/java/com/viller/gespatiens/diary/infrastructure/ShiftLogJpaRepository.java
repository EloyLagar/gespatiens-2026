package com.viller.gespatiens.diary.infrastructure;

import com.viller.gespatiens.diary.domain.DayPart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShiftLogJpaRepository extends JpaRepository<ShiftLogJpaEntity, UUID> {
    List<ShiftLogJpaEntity> findByDate(LocalDate date);
    Optional<ShiftLogJpaEntity> findByDateAndPart(LocalDate date, DayPart part);
}
