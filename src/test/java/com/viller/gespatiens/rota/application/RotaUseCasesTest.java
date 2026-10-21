package com.viller.gespatiens.rota.application;

import com.viller.gespatiens.rota.domain.Employee;
import com.viller.gespatiens.rota.domain.PlannedShift;
import com.viller.gespatiens.rota.domain.RotaRepositories.PlannedShiftRepository;
import com.viller.gespatiens.rota.domain.RotaRepositories.ShiftSwapRepository;
import com.viller.gespatiens.rota.domain.ShiftPart;
import com.viller.gespatiens.rota.domain.ShiftSwap;
import com.viller.gespatiens.rota.domain.SwapNotAllowedException;
import com.viller.gespatiens.rota.domain.SwapStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Test unitario puro: sin Spring ni BD
class RotaUseCasesTest {

    // Miércoles 21 de octubre de 2026
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 21);

    private final Employee pablo = new Employee(UUID.randomUUID(), "Pablo", "SOCIAL_EDUCATOR");
    private final Employee ana = new Employee(UUID.randomUUID(), "Ana", "SOCIAL_EDUCATOR");
    private final Employee marcos = new Employee(UUID.randomUUID(), "Marcos", "PSYCHOLOGIST");

    private final Employee.Directory staff = new Employee.Directory() {
        private final List<Employee> all = List.of(pablo, ana, marcos);

        @Override
        public Optional<Employee> find(UUID id) {
            return all.stream().filter(e -> e.id().equals(id)).findFirst();
        }

        @Override
        public List<Employee> findAll() {
            return all;
        }
    };
    private final InMemoryShifts shifts = new InMemoryShifts();
    private final InMemorySwaps swaps = new InMemorySwaps();

    private final GetRotaUseCase rota = new GetRotaUseCase(shifts, staff);
    private final PlanRotaUseCase plan = new PlanRotaUseCase(shifts, staff);
    private final SwapsUseCase swapsUseCase = new SwapsUseCase(shifts, swaps, staff, CLOCK);

    private List<PlannedShift> of(Employee e) {
        return shifts.all.values().stream().filter(s -> s.isOf(e.id()))
                .sorted(Comparator.comparing(PlannedShift::getDate)).toList();
    }

    @Test
    void theRotaShowsTheWholeStaffAndTheMonthsShifts() {
        plan.assign(TODAY, ShiftPart.MORNING, pablo.id());
        plan.assign(TODAY, ShiftPart.MORNING, pablo.id()); // pulsar dos veces no duplica
        plan.assign(TODAY.plusMonths(1), ShiftPart.NIGHT, ana.id());

        RotaView view = rota.execute(YearMonth.of(2026, 10));

        assertEquals(3, view.employees().size());
        assertEquals(1, view.shifts().size());
        assertThrows(IllegalArgumentException.class, () -> plan.assign(TODAY, ShiftPart.MORNING, UUID.randomUUID()));
    }

    // "Pablo, mañanas, de lunes a viernes, del 1 al 15"
    @Test
    void aRangeIsFilledOnTheChosenWeekdaysWithoutDuplicates() {
        plan.assign(LocalDate.of(2026, 11, 2), ShiftPart.MORNING, pablo.id());

        int added = plan.fill(pablo.id(), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 15),
                EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), Set.of(ShiftPart.MORNING));

        // Del 1 al 15 de noviembre de 2026 hay 10 días laborables; el lunes 2 ya estaba
        assertEquals(9, added);
        assertEquals(10, of(pablo).size());
        assertTrue(of(pablo).stream().noneMatch(s -> s.getDate().getDayOfWeek().getValue() > 5));
        assertEquals(10, plan.clear(pablo.id(), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)));
        assertThrows(IllegalArgumentException.class, () -> plan.fill(pablo.id(), TODAY, TODAY.minusDays(1),
                Set.of(DayOfWeek.MONDAY), Set.of(ShiftPart.MORNING)));
    }

    @Test
    void aWeekIsRepeatedAsAPattern() {
        LocalDate monday = LocalDate.of(2026, 11, 2);
        plan.assign(monday, ShiftPart.MORNING, pablo.id());
        plan.assign(monday.plusDays(2), ShiftPart.NIGHT, ana.id());

        int added = plan.repeatWeek(monday, LocalDate.of(2026, 11, 22));

        // Dos semanas más: el 9 y el 16 (Pablo) y el 11 y el 18 (Ana)
        assertEquals(4, added);
        assertEquals(List.of(monday, monday.plusWeeks(1), monday.plusWeeks(2)),
                of(pablo).stream().map(PlannedShift::getDate).toList());
        assertThrows(IllegalArgumentException.class, () -> plan.repeatWeek(monday.plusDays(1), monday.plusWeeks(3)));
    }

    // El segundo martes va al segundo martes
    @Test
    void aMonthIsCopiedKeepingTheWeekdays() {
        plan.assign(LocalDate.of(2026, 10, 13), ShiftPart.AFTERNOON, ana.id()); // 2º martes de octubre
        plan.assign(LocalDate.of(2026, 10, 29), ShiftPart.MORNING, ana.id()); // 5º jueves: noviembre no lo tiene

        int added = plan.copyMonth(YearMonth.of(2026, 10), YearMonth.of(2026, 11));

        assertEquals(1, added);
        assertEquals(LocalDate.of(2026, 11, 10), of(ana).get(2).getDate());
    }

    // Un cambio de turno: Pablo da su mañana del 25 a Ana a cambio de la noche del 27 de ella
    @Test
    void anAcceptedSwapExchangesTheShifts() {
        PlannedShift mine = plan.assign(LocalDate.of(2026, 10, 25), ShiftPart.MORNING, pablo.id());
        PlannedShift theirs = plan.assign(LocalDate.of(2026, 10, 27), ShiftPart.NIGHT, ana.id());
        PlannedShift other = plan.assign(LocalDate.of(2026, 10, 28), ShiftPart.MORNING, pablo.id());

        SwapView proposed = swapsUseCase.propose(pablo.id(), mine.getId(), ana.id(), theirs.getId());
        // Otro cambio pendiente sobre el mismo turno, que se retira solo al aceptarse el primero
        SwapView competing = swapsUseCase.propose(pablo.id(), mine.getId(), marcos.id(), null);

        assertThrows(SwapNotAllowedException.class, () -> swapsUseCase.accept(proposed.swap().getId(), pablo.id()));
        SwapView accepted = swapsUseCase.accept(proposed.swap().getId(), ana.id());

        assertEquals(SwapStatus.ACCEPTED, accepted.swap().getStatus());
        assertTrue(shifts.all.get(mine.getId()).isOf(ana.id()));
        assertTrue(shifts.all.get(theirs.getId()).isOf(pablo.id()));
        assertTrue(shifts.all.get(other.getId()).isOf(pablo.id()));
        assertEquals(SwapStatus.CANCELLED, swaps.all.get(competing.swap().getId()).getStatus());
        assertEquals(2, swapsUseCase.mine(pablo.id()).size());
    }

    // Que alguien lo cubra: sin turno a cambio
    @Test
    void aColleagueCanJustCoverTheShift() {
        PlannedShift mine = plan.assign(LocalDate.of(2026, 10, 25), ShiftPart.MORNING, pablo.id());

        SwapView proposed = swapsUseCase.propose(pablo.id(), mine.getId(), ana.id(), null);
        swapsUseCase.accept(proposed.swap().getId(), ana.id());

        assertTrue(of(pablo).isEmpty());
        assertEquals(1, of(ana).size());
    }

    @Test
    void nobodyEndsUpWithTheSameShiftTwiceOrSwapsThePast() {
        PlannedShift mine = plan.assign(LocalDate.of(2026, 10, 25), ShiftPart.MORNING, pablo.id());
        plan.assign(LocalDate.of(2026, 10, 25), ShiftPart.MORNING, ana.id());
        PlannedShift past = plan.assign(TODAY.minusDays(1), ShiftPart.MORNING, pablo.id());
        PlannedShift annas = plan.assign(LocalDate.of(2026, 10, 26), ShiftPart.NIGHT, ana.id());

        assertThrows(SwapNotAllowedException.class, () -> swapsUseCase.propose(pablo.id(), mine.getId(), ana.id(), null));
        assertThrows(SwapNotAllowedException.class, () -> swapsUseCase.propose(pablo.id(), past.getId(), marcos.id(), null));
        // Solo de turnos propios
        assertThrows(SwapNotAllowedException.class, () -> swapsUseCase.propose(pablo.id(), annas.getId(), marcos.id(), null));
    }

    @Test
    void theRequesterWithdrawsAndThePartnerDeclines() {
        PlannedShift mine = plan.assign(LocalDate.of(2026, 10, 25), ShiftPart.MORNING, pablo.id());
        UUID first = swapsUseCase.propose(pablo.id(), mine.getId(), ana.id(), null).swap().getId();
        UUID second = swapsUseCase.propose(pablo.id(), mine.getId(), marcos.id(), null).swap().getId();

        assertThrows(SwapNotAllowedException.class, () -> swapsUseCase.cancel(first, ana.id()));
        assertEquals(SwapStatus.CANCELLED, swapsUseCase.cancel(first, pablo.id()).swap().getStatus());
        assertEquals(SwapStatus.DECLINED, swapsUseCase.decline(second, marcos.id()).swap().getStatus());
        assertThrows(SwapNotAllowedException.class, () -> swapsUseCase.accept(second, marcos.id()));
        assertTrue(of(pablo).size() == 1);
    }

    // Si la administración cambia el cuadrante entretanto, el cambio ya no se puede aceptar
    @Test
    void aSwapCannotBeAcceptedOnceTheRotaChanged() {
        PlannedShift mine = plan.assign(LocalDate.of(2026, 10, 25), ShiftPart.MORNING, pablo.id());
        UUID swap = swapsUseCase.propose(pablo.id(), mine.getId(), ana.id(), null).swap().getId();

        plan.unassign(mine.getId());

        assertThrows(SwapNotAllowedException.class, () -> swapsUseCase.accept(swap, ana.id()));
    }

    private static final class InMemoryShifts implements PlannedShiftRepository {
        final Map<UUID, PlannedShift> all = new LinkedHashMap<>();

        @Override
        public PlannedShift save(PlannedShift shift) {
            all.put(shift.getId(), shift);
            return shift;
        }

        @Override
        public Optional<PlannedShift> findById(UUID id) {
            return Optional.ofNullable(all.get(id));
        }

        @Override
        public List<PlannedShift> findBetween(LocalDate from, LocalDate to) {
            return all.values().stream().filter(s -> !s.getDate().isBefore(from) && !s.getDate().isAfter(to))
                    .sorted(Comparator.comparing(PlannedShift::getDate).thenComparing(PlannedShift::getPart)).toList();
        }

        @Override
        public boolean exists(LocalDate date, ShiftPart part, UUID employeeId) {
            return all.values().stream().anyMatch(s -> s.getDate().equals(date) && s.getPart() == part
                    && s.isOf(employeeId));
        }

        @Override
        public void delete(PlannedShift shift) {
            all.remove(shift.getId());
        }
    }

    private static final class InMemorySwaps implements ShiftSwapRepository {
        final Map<UUID, ShiftSwap> all = new LinkedHashMap<>();

        @Override
        public ShiftSwap save(ShiftSwap swap) {
            all.put(swap.getId(), swap);
            return swap;
        }

        @Override
        public Optional<ShiftSwap> findById(UUID id) {
            return Optional.ofNullable(all.get(id));
        }

        @Override
        public List<ShiftSwap> findInvolving(UUID employeeId) {
            return all.values().stream()
                    .filter(s -> s.getRequesterId().equals(employeeId) || s.getPartnerId().equals(employeeId)).toList();
        }

        @Override
        public List<ShiftSwap> findPendingForShifts(Collection<UUID> shiftIds) {
            return all.values().stream()
                    .filter(s -> s.getStatus() == SwapStatus.PENDING && shiftIds.stream().anyMatch(s::involves)).toList();
        }
    }
}
