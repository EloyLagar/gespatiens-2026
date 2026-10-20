package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.domain.PasswordHasher;

// "Hash" legible para los tests: así se puede comprobar qué contraseña se ha guardado
class FakePasswordHasher implements PasswordHasher {

    @Override
    public String hash(String rawPassword) {
        return "hashed:" + rawPassword;
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return hash(rawPassword).equals(passwordHash);
    }
}
