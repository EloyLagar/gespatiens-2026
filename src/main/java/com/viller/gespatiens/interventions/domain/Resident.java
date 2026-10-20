package com.viller.gespatiens.interventions.domain;

import java.util.UUID;

// Lo que las intervenciones necesitan saber de un residente. number es null si ya no está en el centro
public record Resident(UUID id, Integer number, String name, String surname, boolean active) {
}
