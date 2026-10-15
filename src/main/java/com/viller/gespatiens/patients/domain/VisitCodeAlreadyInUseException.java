package com.viller.gespatiens.patients.domain;

public class VisitCodeAlreadyInUseException extends RuntimeException {

    public VisitCodeAlreadyInUseException(String visitCode) {
        super("Ya existe un residente con el código de visita " + visitCode);
    }
}
