package com.viller.gespatiens.dashboard.application;

import com.viller.gespatiens.dashboard.application.DashboardView.WeekCount;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Test unitario puro: sin Spring ni BD
class DashboardUseCaseTest {

    private final List<LocalDate[]> asked = new ArrayList<>();

    private final DashboardQueries queries = new DashboardQueries() {
        @Override
        public List<ShiftCount> shiftsByEmployee(LocalDate from, LocalDate to) {
            asked.add(new LocalDate[]{from, to});
            return List.of(
                    new ShiftCount(UUID.randomUUID(), "Ana", "SOCIAL_EDUCATOR", 2, 1, 0),
                    new ShiftCount(UUID.randomUUID(), "Pablo", "SOCIAL_EDUCATOR", 4, 2, 3),
                    new ShiftCount(UUID.randomUUID(), "Bea", "SOCIAL_EDUCATOR", 1, 1, 1));
        }

        @Override
        public Map<LocalDate, Integer> noticesByWeek(LocalDate fromMonday, LocalDate toSunday) {
            asked.add(new LocalDate[]{fromMonday, toSunday});
            return Map.of(LocalDate.of(2026, 10, 12), 5);
        }

        @Override
        public int activeResidents() {
            return 27;
        }

        @Override
        public int activeResidentsWithoutBothTutors() {
            return 3;
        }

        @Override
        public int reportsInProgress() {
            return 12;
        }

        @Override
        public int interventionsBetween(LocalDate from, LocalDate to) {
            return 40;
        }

        @Override
        public List<OpenMidStayReport> openMidStayReports() {
            return List.of(
                    // Le toca el 1 de noviembre, dentro del plazo de aviso
                    new OpenMidStayReport(resident(4, "Ruiz"), LocalDate.of(2026, 11, 1), REPORT),
                    // Le toca en enero: todavía no se avisa
                    new OpenMidStayReport(resident(7, "Gil"), LocalDate.of(2027, 1, 1), UUID.randomUUID()),
                    // Se le pasó el 1 de septiembre
                    new OpenMidStayReport(resident(9, "Peña"), LocalDate.of(2026, 9, 1), UUID.randomUUID()));
        }

        @Override
        public List<CourtMeasure> courtMeasures() {
            return List.of(
                    new CourtMeasure(resident(2, "Mas"), LocalDate.of(2026, 10, 25), "Juzgado de lo Penal nº 3"),
                    new CourtMeasure(resident(3, "Soler"), LocalDate.of(2027, 3, 1), "Juzgado nº 1"));
        }
    };

    private static final UUID REPORT = UUID.randomUUID();

    private static Deadline.Resident resident(int number, String surname) {
        return new Deadline.Resident(UUID.randomUUID(), number, "Nombre", surname);
    }

    // Hoy es el 21 de octubre de 2026
    private final DashboardUseCase useCase = new DashboardUseCase(queries,
            Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"), ZoneOffset.UTC), Period.ofDays(30));

    // Memoria final, 5.1: "la cantidad de turnos que realizan distintos trabajadores durante un mes"
    @Test
    void shiftsAreForTheMonthFromMostToFewest() {
        DashboardView view = useCase.execute(YearMonth.of(2026, 10));

        assertEquals(LocalDate.of(2026, 10, 1), asked.get(0)[0]);
        assertEquals(LocalDate.of(2026, 10, 31), asked.get(0)[1]);
        assertEquals(List.of("Pablo", "Ana", "Bea"), view.shifts().stream().map(DashboardQueries.ShiftCount::name).toList());
        assertEquals(9, view.shifts().getFirst().total());
    }

    // "La cantidad de avisos por semana": semanas completas de lunes a domingo, también las que no tuvieron
    @Test
    void noticesComeInWholeWeeksThatTouchTheMonth() {
        DashboardView view = useCase.execute(YearMonth.of(2026, 10));

        // Octubre de 2026 empieza en jueves y acaba en sábado
        assertEquals(LocalDate.of(2026, 9, 28), asked.get(1)[0]);
        assertEquals(LocalDate.of(2026, 11, 1), asked.get(1)[1]);
        assertEquals(5, view.noticesByWeek().size());
        assertEquals(new WeekCount(LocalDate.of(2026, 10, 12), 5), view.noticesByWeek().get(2));
        assertEquals(0, view.noticesByWeek().get(0).notices());
    }

    @Test
    void theCurrentFiguresComeAlong() {
        DashboardView view = useCase.execute(YearMonth.of(2026, 10));

        assertEquals(27, view.occupiedPlaces());
        assertEquals(32, view.totalPlaces());
        assertEquals(3, view.residentsWithoutBothTutors());
        assertEquals(12, view.reportsInProgress());
        assertEquals(40, view.interventions());
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null));
    }

    // Lo vencido primero; lo que vence en más de 30 días todavía no sale
    @Test
    void deadlinesWithinTheNoticeAndOverdueOnesFromMostUrgent() {
        List<Deadline> deadlines = useCase.execute(YearMonth.of(2026, 10)).deadlines();

        assertEquals(List.of("Peña", "Mas", "Ruiz"), deadlines.stream().map(d -> d.resident().surname()).toList());
        Deadline overdue = deadlines.get(0);
        assertEquals(Deadline.Kind.MID_STAY_REPORT, overdue.kind());
        assertEquals(LocalDate.of(2026, 9, 1), overdue.dueDate());
        assertEquals(-50, overdue.daysLeft());
        assertTrue(overdue.overdue());
        assertEquals(4, deadlines.get(1).daysLeft());
        assertEquals("Juzgado de lo Penal nº 3", deadlines.get(1).detail());
        assertEquals(REPORT, deadlines.get(2).reportId());
        assertEquals(11, deadlines.get(2).daysLeft());
    }
}
