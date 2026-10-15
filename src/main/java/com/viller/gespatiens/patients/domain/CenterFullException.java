package com.viller.gespatiens.patients.domain;

public class CenterFullException extends RuntimeException {

    public CenterFullException() {
        super("El centro ya tiene " + Patient.MAX_RESIDENTS + " residentes: no se pueden dar más altas");
    }
}
