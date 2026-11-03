package com.viller.gespatiens.access.users.application;

import java.time.Instant;
import java.util.UUID;

// Puerto: avisa de cada intento de entrar, haya ido bien o mal. Quien lleve el registro de accesos lo escucha sin
// que el login dependa de él (ADR 0009)
public interface LoginEvents {

    void attempted(LoginAttempted attempt);

    // email: el que se escribió, normalizado, exista o no. userId: el empleado de ese email, si existe (también
    // cuando la contraseña está mal: así se ve que alguien ha probado su cuenta)
    record LoginAttempted(Instant occurredAt, String email, UUID userId, Outcome outcome, LoginOrigin origin) {
    }

    enum Outcome {
        // Ha entrado
        SUCCESS,
        // Email o contraseña incorrectos
        FAILURE,
        // El fallo que ha agotado los intentos: desde ahora el email queda bloqueado
        LOCKED,
        // Lo ha intentado mientras el email estaba bloqueado: ni se ha comprobado la contraseña
        BLOCKED
    }
}
