package com.viller.gespatiens.diary.domain;

// RF8: otra persona ha guardado el turno o la actividad mientras se editaba. No se pisa lo suyo
public class DiaryConflictException extends RuntimeException {

    public DiaryConflictException(String what) {
        super("Otra persona ha modificado " + what + " mientras lo editabas. Recarga el diario para ver los "
                + "cambios antes de guardar los tuyos");
    }
}
