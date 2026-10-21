package com.viller.gespatiens.dashboard.application;

import java.time.LocalDate;
import java.util.UUID;

// Algo que vence pronto, o que ya ha vencido, de un residente que sigue en el centro.
// reportId: el informe que hay que terminar (solo en MID_STAY_REPORT). detail: el juzgado (solo en COURT_MEASURE_END)
public record Deadline(Kind kind, Resident resident, LocalDate dueDate, long daysLeft, UUID reportId, String detail) {

    public enum Kind {
        // El informe de media estancia sigue en elaboración y se acerca la fecha en que toca
        MID_STAY_REPORT,
        // Termina la medida judicial por la que está en el centro
        COURT_MEASURE_END
    }

    public record Resident(UUID id, Integer number, String name, String surname) {
    }

    public boolean overdue() {
        return daysLeft < 0;
    }
}
