package com.viller.gespatiens.rota.application;

import com.viller.gespatiens.rota.domain.Employee;
import com.viller.gespatiens.rota.domain.PlannedShift;
import com.viller.gespatiens.rota.domain.ShiftSwap;

import java.util.Optional;

// Un cambio de turno con sus personas y sus turnos resueltos. Los turnos pueden faltar si la administración los
// quitó del cuadrante después
public record SwapView(ShiftSwap swap, Optional<Employee> requester, Optional<Employee> partner,
                       Optional<PlannedShift> requesterShift, Optional<PlannedShift> partnerShift) {
}
