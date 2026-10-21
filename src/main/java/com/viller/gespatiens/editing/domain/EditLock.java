package com.viller.gespatiens.editing.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

// RF8: un empleado está editando algo (un apartado de un informe, un turno, una actividad...) y nadie más
// puede editarlo mientras tanto. A diferencia del campo "state" de la aplicación original, el bloqueo caduca
// solo si quien lo tiene deja de renovarlo (cierra la pestaña, se queda sin red, se apaga el equipo):
// nunca se queda un documento inaccesible
public class EditLock {

    // Mientras se edita, la pantalla lo renueva cada 30 segundos
    public static final Duration LEASE = Duration.ofSeconds(90);

    // "report:<uuid>:section:PSYCHO_DIAGNOSIS", "shift:2026-10-21:MORNING"...
    private static final Pattern RESOURCE = Pattern.compile("[A-Za-z0-9_:.-]{1,150}");

    private final String resource;
    private UUID holderId;
    private Instant acquiredAt;
    private Instant expiresAt;
    // Lo lleva la persistencia (bloqueo optimista). null: todavía no se ha guardado nunca
    private final Long revision;

    private EditLock(String resource, UUID holderId, Instant acquiredAt, Instant expiresAt, Long revision) {
        checkResource(resource);
        if (holderId == null || acquiredAt == null || expiresAt == null || !expiresAt.isAfter(acquiredAt)) {
            throw new IllegalArgumentException("El bloqueo necesita quién lo tiene, desde cuándo y hasta cuándo");
        }
        this.resource = resource;
        this.holderId = holderId;
        this.acquiredAt = acquiredAt;
        this.expiresAt = expiresAt;
        this.revision = revision;
    }

    public static EditLock acquire(String resource, UUID holderId, Instant now) {
        return new EditLock(resource, holderId, now, now.plus(LEASE), null);
    }

    public static EditLock reconstitute(String resource, UUID holderId, Instant acquiredAt, Instant expiresAt,
                                        Long revision) {
        return new EditLock(resource, holderId, acquiredAt, expiresAt, revision);
    }

    public static void checkResource(String resource) {
        if (resource == null || !RESOURCE.matcher(resource).matches()) {
            throw new IllegalArgumentException("Recurso no válido: " + resource);
        }
    }

    public boolean isActive(Instant now) {
        return expiresAt.isAfter(now);
    }

    // Otra persona lo tiene y no ha caducado
    public boolean blocks(UUID employeeId, Instant now) {
        return isActive(now) && !holderId.equals(employeeId);
    }

    // Quien ya lo tiene lo renueva; si ha caducado, se lo queda quien lo pide
    public void claim(UUID employeeId, Instant now) {
        if (blocks(employeeId, now)) {
            throw new IllegalStateException("El recurso lo está editando otra persona");
        }
        if (!holderId.equals(employeeId) || !isActive(now)) {
            this.holderId = employeeId;
            this.acquiredAt = now;
        }
        this.expiresAt = now.plus(LEASE);
    }

    public String getResource() {
        return resource;
    }

    public UUID getHolderId() {
        return holderId;
    }

    public Instant getAcquiredAt() {
        return acquiredAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Optional<Long> getRevision() {
        return Optional.ofNullable(revision);
    }
}
