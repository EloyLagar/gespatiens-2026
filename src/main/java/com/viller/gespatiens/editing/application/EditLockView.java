package com.viller.gespatiens.editing.application;

import java.time.Instant;
import java.util.UUID;

// Quién edita algo, desde cuándo y hasta cuándo dura el bloqueo si no lo renueva. mine: es de quien pregunta
public record EditLockView(String resource, UUID holderId, String holderName, Instant since, Instant expiresAt,
                           boolean mine) {
}
