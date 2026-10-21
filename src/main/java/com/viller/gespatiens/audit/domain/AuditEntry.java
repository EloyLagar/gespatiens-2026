package com.viller.gespatiens.audit.domain;

import java.time.Instant;
import java.util.UUID;

// Una línea del registro de cambios: quién hizo qué, cuándo y sobre qué residente. Nunca se modifica, y solo
// se borra al pasar el plazo de conservación (PurgeAuditLogUseCase). Guarda solo identificadores: si se borra al empleado o al residente, la línea
// sigue diciendo que el acceso ocurrió.
// patientId null: no se ha podido saber de qué residente era (p. ej. un informe ya borrado).
// resourceId: el informe o la intervención concretos, si los hay
public class AuditEntry {
    private final UUID id;
    private final Instant occurredAt;
    private final UUID actorId;
    private final AuditAction action;
    private final UUID patientId;
    private final UUID resourceId;

    private AuditEntry(UUID id, Instant occurredAt, UUID actorId, AuditAction action, UUID patientId, UUID resourceId) {
        if (id == null || occurredAt == null || actorId == null || action == null) {
            throw new IllegalArgumentException("Una línea del registro necesita id, momento, empleado y acción");
        }
        this.id = id;
        this.occurredAt = occurredAt;
        this.actorId = actorId;
        this.action = action;
        this.patientId = patientId;
        this.resourceId = resourceId;
    }

    public static AuditEntry record(Instant occurredAt, UUID actorId, AuditAction action, UUID patientId,
                                    UUID resourceId) {
        return new AuditEntry(UUID.randomUUID(), occurredAt, actorId, action, patientId, resourceId);
    }

    public static AuditEntry reconstitute(UUID id, Instant occurredAt, UUID actorId, AuditAction action,
                                          UUID patientId, UUID resourceId) {
        return new AuditEntry(id, occurredAt, actorId, action, patientId, resourceId);
    }

    public UUID getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public UUID getActorId() {
        return actorId;
    }

    public AuditAction getAction() {
        return action;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public UUID getResourceId() {
        return resourceId;
    }
}
