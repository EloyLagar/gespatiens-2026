package com.viller.gespatiens.rota.application;

import com.viller.gespatiens.rota.domain.Employee;
import com.viller.gespatiens.rota.domain.PlannedShift;
import com.viller.gespatiens.rota.domain.RotaRepositories.PlannedShiftRepository;
import com.viller.gespatiens.rota.domain.ShiftPart;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

// La administración planifica el cuadrante (SecurityConfig), de la manera que le venga mejor: turno a turno,
// rellenando un tramo de días, repitiendo una semana o copiando el mes anterior. Rellenar nunca duplica: si el
// trabajador ya tiene ese turno, se deja como está. Devuelve cuántos turnos ha añadido o quitado
public class PlanRotaUseCase {

    // Un tramo razonable: más de un año de golpe es un error de quien lo rellena
    static final int MAX_DAYS = 366;

    private final PlannedShiftRepository shiftRepository;
    private final Employee.Directory employees;

    public PlanRotaUseCase(PlannedShiftRepository shiftRepository, Employee.Directory employees) {
        this.shiftRepository = shiftRepository;
        this.employees = employees;
    }

    // Turno a turno: pulsar una casilla del cuadrante
    public PlannedShift assign(LocalDate date, ShiftPart part, UUID employeeId) {
        if (date == null || part == null) {
            throw new IllegalArgumentException("Indica el día y la parte del día");
        }
        ensureEmployee(employeeId);
        Optional<PlannedShift> existing = shiftRepository.findBetween(date, date).stream()
                .filter(s -> s.getPart() == part && s.isOf(employeeId))
                .findFirst();
        return existing.orElseGet(() -> shiftRepository.save(PlannedShift.plan(date, part, employeeId)));
    }

    public void unassign(UUID shiftId) {
        shiftRepository.findById(shiftId).ifPresent(shiftRepository::delete);
    }

    // Rellenar un tramo: "Pablo, mañanas, de lunes a viernes, del 1 al 15"
    public int fill(UUID employeeId, LocalDate from, LocalDate to, Set<DayOfWeek> weekdays, Set<ShiftPart> parts) {
        ensureEmployee(employeeId);
        checkRange(from, to);
        if (weekdays == null || weekdays.isEmpty() || parts == null || parts.isEmpty()) {
            throw new IllegalArgumentException("Elige al menos un día de la semana y una parte del día");
        }
        int added = 0;
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            if (!weekdays.contains(day.getDayOfWeek())) {
                continue;
            }
            for (ShiftPart part : parts) {
                added += addIfMissing(day, part, employeeId);
            }
        }
        return added;
    }

    // Vaciar los turnos de un trabajador en un tramo (por ejemplo, sus vacaciones)
    public int clear(UUID employeeId, LocalDate from, LocalDate to) {
        ensureEmployee(employeeId);
        checkRange(from, to);
        List<PlannedShift> his = shiftRepository.findBetween(from, to).stream().filter(s -> s.isOf(employeeId)).toList();
        his.forEach(shiftRepository::delete);
        return his.size();
    }

    // Repetir una semana como patrón: los turnos de la semana que empieza en sourceMonday se copian en las semanas
    // siguientes hasta until (incluido)
    public int repeatWeek(LocalDate sourceMonday, LocalDate until) {
        if (sourceMonday == null || sourceMonday.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new IllegalArgumentException("La semana que se repite empieza en lunes");
        }
        LocalDate firstTarget = sourceMonday.plusWeeks(1);
        checkRange(firstTarget, until);
        List<PlannedShift> pattern = shiftRepository.findBetween(sourceMonday, sourceMonday.plusDays(6));
        int added = 0;
        for (LocalDate monday = firstTarget; !monday.isAfter(until); monday = monday.plusWeeks(1)) {
            for (PlannedShift shift : pattern) {
                LocalDate day = monday.plusDays(ChronoUnit.DAYS.between(sourceMonday, shift.getDate()));
                if (!day.isAfter(until)) {
                    added += addIfMissing(day, shift.getPart(), shift.getEmployeeId());
                }
            }
        }
        return added;
    }

    // Copiar un mes en otro: cada turno va al mismo día de la semana de la misma semana del mes (el segundo martes,
    // al segundo martes). Si el mes de destino no tiene ese día (un quinto lunes), se salta
    public int copyMonth(YearMonth from, YearMonth to) {
        if (from == null || to == null || from.equals(to)) {
            throw new IllegalArgumentException("Indica dos meses distintos");
        }
        int added = 0;
        for (PlannedShift shift : shiftRepository.findBetween(from.atDay(1), from.atEndOfMonth())) {
            int nth = (shift.getDate().getDayOfMonth() - 1) / 7;
            LocalDate firstSameWeekday = to.atDay(1);
            while (firstSameWeekday.getDayOfWeek() != shift.getDate().getDayOfWeek()) {
                firstSameWeekday = firstSameWeekday.plusDays(1);
            }
            LocalDate day = firstSameWeekday.plusWeeks(nth);
            if (YearMonth.from(day).equals(to)) {
                added += addIfMissing(day, shift.getPart(), shift.getEmployeeId());
            }
        }
        return added;
    }

    private int addIfMissing(LocalDate day, ShiftPart part, UUID employeeId) {
        if (shiftRepository.exists(day, part, employeeId)) {
            return 0;
        }
        shiftRepository.save(PlannedShift.plan(day, part, employeeId));
        return 1;
    }

    private void ensureEmployee(UUID employeeId) {
        if (employeeId == null || employees.find(employeeId).isEmpty()) {
            throw new IllegalArgumentException("No existe ningún empleado con id " + employeeId);
        }
    }

    private static void checkRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new IllegalArgumentException("Indica un tramo de fechas válido");
        }
        if (ChronoUnit.DAYS.between(from, to) >= MAX_DAYS) {
            throw new IllegalArgumentException("El tramo no puede pasar de un año");
        }
    }
}
