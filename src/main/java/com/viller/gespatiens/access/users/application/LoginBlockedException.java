package com.viller.gespatiens.access.users.application;

import java.time.Duration;
import java.time.Instant;

// Demasiados intentos fallidos seguidos con el mismo email: no se deja probar más hasta que pase el bloqueo
public class LoginBlockedException extends RuntimeException {

    private final Instant until;

    public LoginBlockedException(Instant until, Instant now) {
        super("Demasiados intentos fallidos. Vuelve a intentarlo dentro de "
                + Math.max(1, (Duration.between(now, until).toSeconds() + 59) / 60) + " minutos");
        this.until = until;
    }

    public Instant getUntil() {
        return until;
    }
}
