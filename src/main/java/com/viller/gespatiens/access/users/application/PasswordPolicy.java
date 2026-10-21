package com.viller.gespatiens.access.users.application;

// Reglas de la contraseña en claro. Se validan en la capa de aplicación porque,
// una vez hasheada, el dominio ya no puede saber cómo era
final class PasswordPolicy {

    static final int MIN_PASSWORD_LENGTH = 8;

    private PasswordPolicy() {
    }

    static void check(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres");
        }
    }
}
