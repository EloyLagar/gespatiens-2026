package com.viller.gespatiens.rota.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Los puertos de persistencia del cuadrante
public final class RotaRepositories {

    private RotaRepositories() {
    }

    public interface PlannedShiftRepository {
        PlannedShift save(PlannedShift shift);
        Optional<PlannedShift> findById(UUID id);
        // Los de varios días (ambos incluidos), por día y parte del día
        List<PlannedShift> findBetween(LocalDate from, LocalDate to);
        boolean exists(LocalDate date, ShiftPart part, UUID employeeId);
        void delete(PlannedShift shift);
    }

    public interface ShiftSwapRepository {
        ShiftSwap save(ShiftSwap swap);
        Optional<ShiftSwap> findById(UUID id);
        // Los que propuso o le proponen a un trabajador, del más reciente al más antiguo
        List<ShiftSwap> findInvolving(UUID employeeId);
        // Los pendientes que tocan alguno de estos turnos
        List<ShiftSwap> findPendingForShifts(Collection<UUID> shiftIds);
    }
}
