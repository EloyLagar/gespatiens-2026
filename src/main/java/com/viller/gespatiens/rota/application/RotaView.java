package com.viller.gespatiens.rota.application;

import com.viller.gespatiens.rota.domain.Employee;
import com.viller.gespatiens.rota.domain.PlannedShift;

import java.time.YearMonth;
import java.util.List;

// El cuadrante de un mes: toda la plantilla (también quien no tiene turnos, para poder asignárselos) y los turnos
public record RotaView(YearMonth month, List<Employee> employees, List<PlannedShift> shifts) {
}
