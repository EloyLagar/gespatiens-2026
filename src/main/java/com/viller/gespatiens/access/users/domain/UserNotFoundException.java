package com.viller.gespatiens.access.users.domain;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(UUID id) {
        super("No existe ningún usuario con id " + id);
    }

    public UserNotFoundException(String email) {
        super("No existe ningún usuario con el email " + email);
    }
}
