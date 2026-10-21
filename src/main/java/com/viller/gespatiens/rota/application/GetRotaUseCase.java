package com.viller.gespatiens.rota.application;

import com.viller.gespatiens.rota.domain.Employee;
import com.viller.gespatiens.rota.domain.RotaRepositories.PlannedShiftRepository;

import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

// El cuadrante de un mes. Lo consulta todo el personal: cada uno ve sus turnos y con quién coincide
public class GetRotaUseCase {

    private final PlannedShiftRepository shiftRepository;
    private final Employee.Directory employees;

    public GetRotaUseCase(PlannedShiftRepository shiftRepository, Employee.Directory employees) {
        this.shiftRepository = shiftRepository;
        this.employees = employees;
    }

    public RotaView execute(YearMonth month) {
        if (month == null) {
            throw new IllegalArgumentException("Indica el mes");
        }
        List<Employee> staff = employees.findAll().stream()
                .sorted(Comparator.comparing(Employee::speciality).thenComparing(Employee::name,
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new RotaView(month, staff, shiftRepository.findBetween(month.atDay(1), month.atEndOfMonth()));
    }
}
