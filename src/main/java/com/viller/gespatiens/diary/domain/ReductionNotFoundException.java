package com.viller.gespatiens.diary.domain;

import java.time.LocalDate;
import java.util.UUID;

public class ReductionNotFoundException extends RuntimeException {

    public ReductionNotFoundException(LocalDate date, UUID patientId) {
        super("No hay ninguna rebaja del residente " + patientId + " el " + date);
    }
}
