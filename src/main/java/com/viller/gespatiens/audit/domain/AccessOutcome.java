package com.viller.gespatiens.audit.domain;

// Cómo acabó un intento de entrar
public enum AccessOutcome {
    // Entró
    SUCCESS,
    // Email o contraseña incorrectos
    FAILURE,
    // El fallo que agotó los intentos: el email quedó bloqueado un rato
    LOCKED,
    // Lo intentó con el email bloqueado: ni se comprobó la contraseña
    BLOCKED
}
