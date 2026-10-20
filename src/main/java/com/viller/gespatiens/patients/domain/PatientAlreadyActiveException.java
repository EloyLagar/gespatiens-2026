package com.viller.gespatiens.patients.domain;

import java.util.UUID;

public class PatientAlreadyActiveException extends RuntimeException {

    public PatientAlreadyActiveException(UUID id) {
        super("El residente " + id + " ya está dado de alta en el centro");
    }
}
