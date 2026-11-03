package com.viller.gespatiens.audit.domain;

import java.util.Set;

// Qué pasó: cómo acabó un intento de entrar, o qué se hizo con una contraseña
public enum AccessOutcome {
    // Entró
    SUCCESS,
    // Email o contraseña incorrectos
    FAILURE,
    // El fallo que agotó los intentos: el email quedó bloqueado un rato
    LOCKED,
    // Lo intentó con el email bloqueado: ni se comprobó la contraseña
    BLOCKED,
    // Cambió su contraseña desde Mi perfil
    PASSWORD_CHANGED,
    // Intentó cambiarla con una contraseña actual que no era
    PASSWORD_CHANGE_REJECTED,
    // La creó desde el enlace del correo
    PASSWORD_SET,
    // La administración le puso una provisional
    PASSWORD_RESET;

    public static final Set<AccessOutcome> FAILED_LOGINS = Set.of(FAILURE, LOCKED, BLOCKED);
    public static final Set<AccessOutcome> PASSWORD_EVENTS =
            Set.of(PASSWORD_CHANGED, PASSWORD_CHANGE_REJECTED, PASSWORD_SET, PASSWORD_RESET);
}
