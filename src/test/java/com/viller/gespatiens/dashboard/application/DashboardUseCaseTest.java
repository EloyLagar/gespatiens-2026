package com.viller.gespatiens.dashboard.application;

import com.viller.gespatiens.dashboard.application.DashboardView.WeekCount;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    };

    private final DashboardUseCase useCase = new DashboardUseCase(queries);

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
}
