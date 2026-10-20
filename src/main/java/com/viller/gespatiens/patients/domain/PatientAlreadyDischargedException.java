package com.viller.gespatiens.patients.domain;

import java.util.UUID;

public class PatientAlreadyDischargedException extends RuntimeException {

    public PatientAlreadyDischargedException(UUID id) {
        super("El residente " + id + " ya está dado de baja");
    }
}
