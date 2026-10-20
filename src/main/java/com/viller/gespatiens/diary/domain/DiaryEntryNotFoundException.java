package com.viller.gespatiens.diary.domain;

import java.util.UUID;

// Una actividad o salida del diario que no existe (o ya se ha borrado)
public class DiaryEntryNotFoundException extends RuntimeException {

    public DiaryEntryNotFoundException(String what, UUID id) {
        super("No existe ninguna " + what + " con id " + id);
    }
}
