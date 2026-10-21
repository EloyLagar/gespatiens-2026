package com.viller.gespatiens.access.users.domain;

import java.util.UUID;

public class SignatureNotFoundException extends RuntimeException {

    public SignatureNotFoundException(UUID userId) {
        super("El empleado " + userId + " todavía no ha subido su firma");
    }
}
