package com.viller.gespatiens.patients.domain;

import java.util.UUID;

public class AuthorizedVisitorNotFoundException extends RuntimeException {

    public AuthorizedVisitorNotFoundException(UUID id) {
        super("No existe ninguna persona autorizada con id " + id);
    }
}
