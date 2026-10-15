package com.viller.gespatiens.access.users.domain;

// Puerto: el dominio necesita hashear contraseñas pero no sabe con qué algoritmo.
// La implementación (BCrypt de Spring Security) vive en infrastructure.
public interface PasswordHasher {
    String hash(String rawPassword);
}
