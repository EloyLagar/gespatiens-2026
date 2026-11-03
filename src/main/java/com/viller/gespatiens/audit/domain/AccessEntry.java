package com.viller.gespatiens.audit.domain;

import java.time.Instant;
import java.util.UUID;

// Una línea del registro de accesos: un intento de entrar en la aplicación, haya ido bien o mal (ENS op.acc,
// op.exp.8). Nunca se modifica, y solo se borra al pasar el plazo de conservación (PurgeAccessLogUseCase).
// email: el que se escribió, exista o no. userId: el empleado de ese email, si existía; null si no es de nadie.
// ipAddress y userAgent: desde dónde, si se sabe
public class AccessEntry {

    public static final int MAX_EMAIL = 254;
    public static final int MAX_USER_AGENT = 255;

    private final UUID id;
    private final Instant occurredAt;
    private final String email;
    private final UUID userId;
    private final AccessOutcome outcome;
    private final String ipAddress;
    private final String userAgent;

    private AccessEntry(UUID id, Instant occurredAt, String email, UUID userId, AccessOutcome outcome,
                        String ipAddress, String userAgent) {
        if (id == null || occurredAt == null || email == null || outcome == null) {
            throw new IllegalArgumentException("Una línea del registro de accesos necesita id, momento, email y resultado");
        }
        this.id = id;
        this.occurredAt = occurredAt;
        this.email = cut(email, MAX_EMAIL);
        this.userId = userId;
        this.outcome = outcome;
        this.ipAddress = ipAddress;
        this.userAgent = cut(userAgent, MAX_USER_AGENT);
    }

    // Quien prueba contraseñas puede mandar cualquier cosa como email: se guarda recortado, nunca se rechaza
    public static AccessEntry record(Instant occurredAt, String email, UUID userId, AccessOutcome outcome,
                                     String ipAddress, String userAgent) {
        return new AccessEntry(UUID.randomUUID(), occurredAt, email, userId, outcome, ipAddress, userAgent);
    }

    public static AccessEntry reconstitute(UUID id, Instant occurredAt, String email, UUID userId,
                                           AccessOutcome outcome, String ipAddress, String userAgent) {
        return new AccessEntry(id, occurredAt, email, userId, outcome, ipAddress, userAgent);
    }

    private static String cut(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }

    public UUID getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getEmail() {
        return email;
    }

    public UUID getUserId() {
        return userId;
    }

    public AccessOutcome getOutcome() {
        return outcome;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }
}
