package com.viller.gespatiens.access.users.application;

import java.time.Instant;
import java.util.Optional;

// Puerto: los intentos fallidos de entrar con cada email. La clave es el email tal cual se escribió
// (normalizado), exista o no: así el bloqueo no delata qué emails están registrados
public interface LoginAttempts {

    // Hasta cuándo está bloqueado, si lo está
    Optional<Instant> blockedUntil(String email, Instant now);

    void recordFailure(String email, Instant now);

    void reset(String email);
}
