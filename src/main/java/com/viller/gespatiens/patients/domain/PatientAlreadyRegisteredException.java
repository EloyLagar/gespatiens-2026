package com.viller.gespatiens.patients.domain;

public class PatientAlreadyRegisteredException extends RuntimeException {

    public PatientAlreadyRegisteredException(Dni dni) {
        super("Ya existe un residente con el DNI " + dni.value());
    }
}
