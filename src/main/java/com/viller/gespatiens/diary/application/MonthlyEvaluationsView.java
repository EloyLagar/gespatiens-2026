package com.viller.gespatiens.diary.application;

import com.viller.gespatiens.diary.domain.ActivityType;
import com.viller.gespatiens.diary.domain.Resident;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// La tabla mensual de valoraciones de un tipo de actividad (memoria final, 4.6.8): una fila por residente y una
// columna por día del mes. Las medias semanales y la del mes las calcula la pantalla con estas notas
public record MonthlyEvaluationsView(YearMonth month, ActivityType type, List<DayColumn> days,
                                     List<ResidentRow> rows) {

    // Las actividades de ese tipo que hubo ese día (normalmente una, a veces ninguna)
    public record DayColumn(LocalDate date, List<ActivityRef> activities) {
    }

    // version: la que hay que mandar al cambiar una nota (RF8)
    public record ActivityRef(UUID id, LocalTime startTime, Optional<String> title, long version) {
    }

    // Una celda por día, en el mismo orden que days
    public record ResidentRow(Resident resident, List<Cell> cells) {
    }

    // mark: la nota (la media si ese día hubo varias actividades del tipo). attended: asistió a alguna.
    // activityId: la actividad cuya nota se cambia desde la tabla; solo si ese día hubo una y asistió
    public record Cell(Optional<BigDecimal> mark, boolean attended, Optional<UUID> activityId) {
    }
}
