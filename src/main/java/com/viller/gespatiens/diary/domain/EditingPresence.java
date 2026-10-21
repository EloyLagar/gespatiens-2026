package com.viller.gespatiens.diary.domain;

import java.time.LocalDate;
import java.util.UUID;

// Puerto hacia el módulo de bloqueos de edición (RF8): antes de guardar un turno o una actividad se comprueba
// que no lo esté editando otra persona. Si lo está, lanza la excepción de ese módulo (409 con su nombre)
public interface EditingPresence {

    void ensureNotEditedByOther(String resource, UUID employeeId);

    // Los nombres de lo que se bloquea en el diario. La pantalla usa los mismos
    static String shift(LocalDate date, DayPart part) {
        return "shift:" + date + ":" + part.name();
    }

    // La actividad entera: hora, tipo, lista de asistencia y valoraciones
    static String activity(UUID activityId) {
        return "activity:" + activityId;
    }
}
