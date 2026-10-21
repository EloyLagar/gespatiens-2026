package com.viller.gespatiens.reports.domain;

import java.util.UUID;

public class ResidentNotFoundException extends RuntimeException {

    public ResidentNotFoundException(UUID patientId) {
        super("No existe ningún residente con id " + patientId);
    }
}
