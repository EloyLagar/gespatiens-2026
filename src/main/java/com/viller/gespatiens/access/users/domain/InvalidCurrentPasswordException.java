package com.viller.gespatiens.access.users.domain;

public class InvalidCurrentPasswordException extends RuntimeException {

    public InvalidCurrentPasswordException() {
        super("La contraseña actual no es correcta");
    }
}
