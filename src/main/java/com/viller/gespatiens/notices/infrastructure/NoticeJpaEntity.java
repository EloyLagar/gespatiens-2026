package com.viller.gespatiens.notices.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notices")
public class NoticeJpaEntity {

    // El id lo genera el dominio (UUID), no la base de datos: sin @GeneratedValue
    @Id
    private UUID id;

    // Solo el id: los avisos no cargan la entidad del empleado (otro módulo)
    private UUID authorId;

    @Column(nullable = false, length = 2000)
    private String text;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant editedAt;

    public UUID getId() {
        return id;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public String getText() {
        return text;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getEditedAt() {
        return editedAt;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setEditedAt(Instant editedAt) {
        this.editedAt = editedAt;
    }
}
