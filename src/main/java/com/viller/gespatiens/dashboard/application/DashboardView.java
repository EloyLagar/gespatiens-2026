package com.viller.gespatiens.dashboard.application;

import com.viller.gespatiens.dashboard.application.DashboardQueries.ShiftCount;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

// El panel de control de un mes. Las cifras de "ahora" (plazas, tutores, informes a medias) son las de hoy;
// las del mes (turnos, avisos, intervenciones), las de ese mes
public record DashboardView(
        YearMonth month,
        int occupiedPlaces,
        int totalPlaces,
        int residentsWithoutBothTutors,
        int reportsInProgress,
        int interventions,
        // De quien más turnos hizo a quien menos
        List<ShiftCount> shifts,
        // Las semanas que tocan el mes, de lunes a domingo, en orden; las que no tuvieron avisos, a 0
        List<WeekCount> noticesByWeek) {

    public record WeekCount(LocalDate monday, int notices) {
    }
}
