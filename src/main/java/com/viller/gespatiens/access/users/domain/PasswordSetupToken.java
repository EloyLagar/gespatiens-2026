package com.viller.gespatiens.access.users.domain;

import java.time.Instant;
import java.util.UUID;

// El enlace del correo con el que un empleado crea su contraseña. Solo se guarda el hash del token:
// quien lea la base de datos no puede usar los enlaces que siguen vigentes
public class PasswordSetupToken {
    private final String tokenHash;
    private final UUID userId;
    private final Instant expiresAt;

    private PasswordSetupToken(String tokenHash, UUID userId, Instant expiresAt) {
        if (tokenHash == null || tokenHash.isBlank()) {
            throw new IllegalArgumentException("El hash del token no puede estar vacío");
        }
        if (userId == null) {
            throw new IllegalArgumentException("El token debe ser de un empleado");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("El token debe caducar");
        }
        this.tokenHash = tokenHash;
        this.userId = userId;
        this.expiresAt = expiresAt;
    }

    public static PasswordSetupToken issue(String tokenHash, UUID userId, Instant expiresAt) {
        return new PasswordSetupToken(tokenHash, userId, expiresAt);
    }

    public static PasswordSetupToken reconstitute(String tokenHash, UUID userId, Instant expiresAt) {
        return new PasswordSetupToken(tokenHash, userId, expiresAt);
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public UUID getUserId() {
        return userId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
