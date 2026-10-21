package com.viller.gespatiens.editing.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "edit_locks")
public class EditLockJpaEntity {

    @Id
    @Column(length = 150)
    private String resource;

    @Column(nullable = false)
    private UUID holderId;

    @Column(nullable = false)
    private Instant acquiredAt;

    @Column(nullable = false)
    private Instant expiresAt;

    // null mientras no se ha guardado nunca: así Spring Data sabe que es nuevo y hace un INSERT, que choca
    // con la clave primaria si otro empleado lo ha cogido en el mismo instante
    @Version
    private Long revision;

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public UUID getHolderId() {
        return holderId;
    }

    public void setHolderId(UUID holderId) {
        this.holderId = holderId;
    }

    public Instant getAcquiredAt() {
        return acquiredAt;
    }

    public void setAcquiredAt(Instant acquiredAt) {
        this.acquiredAt = acquiredAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getRevision() {
        return revision;
    }

    public void setRevision(Long revision) {
        this.revision = revision;
    }
}
