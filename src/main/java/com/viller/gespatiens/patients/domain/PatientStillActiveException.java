package com.viller.gespatiens.patients.domain;

import java.util.UUID;

public class PatientStillActiveException extends RuntimeException {

    public PatientStillActiveException(UUID id) {
        super("El residente " + id + " sigue en el centro: hay que darlo de baja antes de borrarlo");
    }
}
