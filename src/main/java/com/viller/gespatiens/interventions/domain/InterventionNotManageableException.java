package com.viller.gespatiens.interventions.domain;

public class InterventionNotManageableException extends RuntimeException {

    public InterventionNotManageableException() {
        super("Solo el autor de la intervención o la administración pueden modificarla o eliminarla");
    }
}
