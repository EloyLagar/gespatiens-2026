package com.viller.gespatiens.dashboard.application;

import com.viller.gespatiens.dashboard.application.DashboardQueries.ShiftCount;
import com.viller.gespatiens.dashboard.application.DashboardView.WeekCount;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

// Memoria final, 5.1: "un panel de administración al que solo esta pudiese acceder y ver distinta información,
// por ejemplo: la cantidad de turnos que realizan distintos trabajadores durante un mes, la cantidad de avisos
// por semana, etc." Quién puede verlo lo comprueba SecurityConfig
public class DashboardUseCase {

    // RF10: las plazas del centro
    public static final int TOTAL_PLACES = 32;

    private final DashboardQueries queries;
    private final Clock clock;
    // Con cuánta antelación se avisa de lo que vence
    private final Period notice;

    public DashboardUseCase(DashboardQueries queries, Clock clock, Period notice) {
        this.queries = queries;
        this.clock = clock;
        this.notice = notice;
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
                queries.interventionsBetween(first, last), shifts, weeks, deadlines());
    }

    // Lo que vence en el plazo de aviso y lo que ya ha vencido, de lo más urgente a lo menos. No depende del
    // mes elegido: es trabajo pendiente de hoy
    private List<Deadline> deadlines() {
        LocalDate today = LocalDate.now(clock);
        LocalDate horizon = today.plus(notice);
        Stream<Deadline> reports = queries.openMidStayReports().stream()
                .map(r -> deadline(Deadline.Kind.MID_STAY_REPORT, r.resident(), r.dueDate(), today, r.reportId(), null));
        Stream<Deadline> measures = queries.courtMeasures().stream()
                .map(m -> deadline(Deadline.Kind.COURT_MEASURE_END, m.resident(), m.endDate(), today, null, m.court()));
        return Stream.concat(reports, measures)
                .filter(d -> !d.dueDate().isAfter(horizon))
                .sorted(Comparator.comparing(Deadline::dueDate).thenComparing(d -> d.resident().surname()))
                .toList();
    }

    private static Deadline deadline(Deadline.Kind kind, Deadline.Resident resident, LocalDate due, LocalDate today,
                                     UUID reportId, String detail) {
        return new Deadline(kind, resident, due, ChronoUnit.DAYS.between(today, due), reportId, detail);
    }
}
