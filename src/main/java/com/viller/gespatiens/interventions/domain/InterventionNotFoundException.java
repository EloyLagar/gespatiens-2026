package com.viller.gespatiens.interventions.domain;

import java.util.UUID;

public class InterventionNotFoundException extends RuntimeException {

    public InterventionNotFoundException(UUID id) {
        super("No existe ninguna intervención con id " + id);
    }
}
