package com.viller.gespatiens.access.users.domain;

public class PhoneNumberAlreadyRegisteredException extends RuntimeException {

    public PhoneNumberAlreadyRegisteredException(String phoneNumber) {
        super("Ya existe un usuario con el teléfono " + phoneNumber);
    }
}
