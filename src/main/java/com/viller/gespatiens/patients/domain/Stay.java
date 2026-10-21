package com.viller.gespatiens.patients.domain;

import java.time.LocalDate;

// Una estancia en el centro. exitDate null: la estancia en curso de quien sigue en el centro
public record Stay(LocalDate entryDate, LocalDate exitDate) {

    public Stay {
        if (entryDate == null) {
            throw new IllegalArgumentException("La estancia necesita fecha de entrada");
        }
        if (exitDate != null && exitDate.isBefore(entryDate)) {
            throw new IllegalArgumentException("La salida de una estancia no puede ser anterior a su entrada");
        }
    }

    public boolean ongoing() {
        return exitDate == null;
    }
}
