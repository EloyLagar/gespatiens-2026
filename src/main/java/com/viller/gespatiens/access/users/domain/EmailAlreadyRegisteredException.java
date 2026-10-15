package com.viller.gespatiens.access.users.domain;

public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException(String email) {
        super("Ya existe un usuario con el email " + email);
    }
}
