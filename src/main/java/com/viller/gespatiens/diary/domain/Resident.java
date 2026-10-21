package com.viller.gespatiens.diary.domain;

import java.util.UUID;

// Lo que el diario necesita saber de un residente. number es null si ya no está en el centro
public record Resident(UUID id, Integer number, String name, String surname, boolean active) {
}
