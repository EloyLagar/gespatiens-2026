package com.viller.gespatiens.dashboard.application;

import com.viller.gespatiens.dashboard.application.DashboardQueries.ShiftCount;
import com.viller.gespatiens.dashboard.application.DashboardView.WeekCount;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

// Memoria final, 5.1: "un panel de administración al que solo esta pudiese acceder y ver distinta información,
// por ejemplo: la cantidad de turnos que realizan distintos trabajadores durante un mes, la cantidad de avisos
// por semana, etc." Quién puede verlo lo comprueba SecurityConfig
public class DashboardUseCase {

    // RF10: las plazas del centro
    public static final int TOTAL_PLACES = 32;

    private final DashboardQueries queries;

    public DashboardUseCase(DashboardQueries queries) {
        this.queries = queries;
    }

    public DashboardView execute(YearMonth month) {
        if (month == null) {
            throw new IllegalArgumentException("Indica el mes");
        }
        LocalDate first = month.atDay(1);
        LocalDate last = month.atEndOfMonth();

        List<ShiftCount> shifts = queries.shiftsByEmployee(first, last).stream()
                .sorted(Comparator.comparingInt(ShiftCount::total).reversed()
                        .thenComparing(ShiftCount::name, String.CASE_INSENSITIVE_ORDER))
                .toList();

        // Semanas completas (de lunes a domingo) que tocan el mes: los avisos de una semana a caballo entre dos
        // meses cuentan enteros, para que todas las barras midan lo mismo
        LocalDate firstMonday = first.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate lastSunday = last.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        Map<LocalDate, Integer> notices = queries.noticesByWeek(firstMonday, lastSunday);
        List<WeekCount> weeks = firstMonday.datesUntil(lastSunday.plusDays(1), Period.ofWeeks(1))
                .map(monday -> new WeekCount(monday, notices.getOrDefault(monday, 0)))
                .toList();

        return new DashboardView(month, queries.activeResidents(), TOTAL_PLACES,
                queries.activeResidentsWithoutBothTutors(), queries.reportsInProgress(),
                queries.interventionsBetween(first, last), shifts, weeks);
    }
}
