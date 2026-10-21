package com.viller.gespatiens.rota.application;

import com.viller.gespatiens.rota.domain.Employee;
import com.viller.gespatiens.rota.domain.PlannedShift;
import com.viller.gespatiens.rota.domain.RotaRepositories.PlannedShiftRepository;
import com.viller.gespatiens.rota.domain.RotaRepositories.ShiftSwapRepository;
import com.viller.gespatiens.rota.domain.ShiftSwap;
import com.viller.gespatiens.rota.domain.SwapNotAllowedException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Los cambios de turno entre trabajadores, sin que intervenga la administración (decisión del centro)
public class SwapsUseCase {

    private final PlannedShiftRepository shiftRepository;
    private final ShiftSwapRepository swapRepository;
    private final Employee.Directory employees;
    private final Clock clock;

    public SwapsUseCase(PlannedShiftRepository shiftRepository, ShiftSwapRepository swapRepository,
                        Employee.Directory employees, Clock clock) {
        this.shiftRepository = shiftRepository;
        this.swapRepository = swapRepository;
        this.employees = employees;
        this.clock = clock;
    }

    // partnerShiftId null: el compañero solo cubre el turno, sin dar uno suyo a cambio
    public SwapView propose(UUID requesterId, UUID myShiftId, UUID partnerId, UUID partnerShiftId) {
        if (partnerId == null || employees.find(partnerId).isEmpty()) {
            throw new IllegalArgumentException("Elige con qué compañero quieres cambiar el turno");
        }
        PlannedShift mine = shift(myShiftId);
        PlannedShift theirs = partnerShiftId == null ? null : shift(partnerShiftId);
        ensureNoClash(mine, theirs, requesterId, partnerId);
        ShiftSwap swap = ShiftSwap.propose(mine, requesterId, partnerId, theirs, clock.instant(), today());
        return view(swapRepository.save(swap));
    }

    public SwapView accept(UUID swapId, UUID employeeId) {
        ShiftSwap swap = swap(swapId);
        PlannedShift mine = shiftRepository.findById(swap.getRequesterShiftId()).orElse(null);
        PlannedShift theirs = swap.getPartnerShiftId().flatMap(shiftRepository::findById).orElse(null);
        if (mine != null) {
            ensureNoClash(mine, theirs, swap.getRequesterId(), swap.getPartnerId());
        }
        Instant now = clock.instant();
        swap.accept(employeeId, mine, theirs, now, today());
        shiftRepository.save(mine);
        if (theirs != null) {
            shiftRepository.save(theirs);
        }
        ShiftSwap saved = swapRepository.save(swap);
        // Los demás cambios pendientes de estos turnos ya no tienen sentido: los turnos han cambiado de manos
        List<UUID> moved = new ArrayList<>(List.of(mine.getId()));
        if (theirs != null) {
            moved.add(theirs.getId());
        }
        swapRepository.findPendingForShifts(moved).forEach(other -> {
            other.withdrawBecauseOutdated(now);
            swapRepository.save(other);
        });
        return view(saved);
    }

    public SwapView decline(UUID swapId, UUID employeeId) {
        ShiftSwap swap = swap(swapId);
        swap.decline(employeeId, clock.instant());
        return view(swapRepository.save(swap));
    }

    public SwapView cancel(UUID swapId, UUID employeeId) {
        ShiftSwap swap = swap(swapId);
        swap.cancel(employeeId, clock.instant());
        return view(swapRepository.save(swap));
    }

    // Los que ha propuesto y los que le proponen, del más reciente al más antiguo
    public List<SwapView> mine(UUID employeeId) {
        return swapRepository.findInvolving(employeeId).stream().map(this::view).toList();
    }

    // Que nadie acabe con dos veces el mismo turno: el compañero no puede tener ya el turno que recibe, ni quien lo
    // propone el que recibe a cambio
    private void ensureNoClash(PlannedShift mine, PlannedShift theirs, UUID requesterId, UUID partnerId) {
        if (shiftRepository.exists(mine.getDate(), mine.getPart(), partnerId)) {
            throw new SwapNotAllowedException("Ese compañero ya trabaja ese turno");
        }
        if (theirs != null && shiftRepository.exists(theirs.getDate(), theirs.getPart(), requesterId)) {
            throw new SwapNotAllowedException("Ya trabajas el turno que pides a cambio");
        }
    }

    private PlannedShift shift(UUID id) {
        return shiftRepository.findById(id)
                .orElseThrow(() -> new SwapNotAllowedException("Ese turno ya no está en el cuadrante"));
    }

    private ShiftSwap swap(UUID id) {
        return swapRepository.findById(id)
                .orElseThrow(() -> new SwapNotAllowedException("Ese cambio de turno no existe"));
    }

    private SwapView view(ShiftSwap swap) {
        return new SwapView(swap, employees.find(swap.getRequesterId()), employees.find(swap.getPartnerId()),
                shiftRepository.findById(swap.getRequesterShiftId()),
                swap.getPartnerShiftId().flatMap(shiftRepository::findById));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
