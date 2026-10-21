package com.viller.gespatiens.rota.infrastructure;

import com.viller.gespatiens.rota.domain.ShiftPart;
import com.viller.gespatiens.rota.infrastructure.RotaJpaEntities.PlannedShiftJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PlannedShiftJpaRepository extends JpaRepository<PlannedShiftJpaEntity, UUID> {

    List<PlannedShiftJpaEntity> findByDateBetweenOrderByDateAscPartAsc(LocalDate from, LocalDate to);

    boolean existsByDateAndPartAndEmployeeId(LocalDate date, ShiftPart part, UUID employeeId);
}
