package com.viller.gespatiens.patients.domain;

import java.util.UUID;

public class PatientNotFoundException extends RuntimeException {

    public PatientNotFoundException(UUID id) {
        super("No existe ningún residente con id " + id);
    }
}
