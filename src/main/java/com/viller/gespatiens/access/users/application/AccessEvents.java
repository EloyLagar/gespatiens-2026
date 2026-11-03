package com.viller.gespatiens.access.users.application;

import java.time.Instant;
import java.util.UUID;

// Puerto: avisa de lo que pasa con el acceso a la aplicación: cada intento de entrar, haya ido bien o mal, y cada
// cambio de contraseña. Quien lleve el registro de accesos lo escucha sin que este módulo dependa de él (ADR 0009)
public interface AccessEvents {

    void loginAttempted(LoginAttempted attempt);

    void passwordChanged(PasswordChanged change);

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

    // userId y email: de quién es la contraseña. actorId: quién la ha cambiado (él mismo o la administración)
    record PasswordChanged(Instant occurredAt, UUID userId, String email, PasswordChange kind, UUID actorId,
                           LoginOrigin origin) {
    }

    enum PasswordChange {
        // El empleado ha cambiado la suya desde Mi perfil
        CHANGED,
        // Lo ha intentado con una contraseña actual que no era: puede ser alguien en una sesión ajena
        CHANGE_REJECTED,
        // La ha creado desde el enlace del correo (al darle de alta o al pedir una nueva)
        SET_FROM_LINK,
        // La administración le ha puesto una provisional
        RESET_BY_ADMIN
    }
}
