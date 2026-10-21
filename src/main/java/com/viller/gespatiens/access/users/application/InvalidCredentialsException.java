package com.viller.gespatiens.access.users.application;

// Mismo mensaje si el email no existe o si la contraseña es incorrecta:
// así nadie puede averiguar qué emails están registrados probando el login
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Email o contraseña incorrectos");
    }
}
