package com.viller.gespatiens.notices.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

// RF5: un aviso de un empleado para el resto del personal. Formato de la memoria: autor, fecha y texto
public class Notice {

    public static final int MAX_LENGTH = 2000;

    private final UUID id;
    // null si el autor ya no está en la plantilla (la BD lo pone a null al borrar al empleado)
    private final UUID authorId;
    private String text;
    private final Instant createdAt;
    private Instant editedAt;

    private Notice(UUID id, UUID authorId, String text, Instant createdAt, Instant editedAt) {
        if (id == null) {
            throw new IllegalArgumentException("El id no puede ser null");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("La fecha del aviso no puede estar vacía");
        }
        if (editedAt != null && editedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Un aviso no puede modificarse antes de publicarse");
        }
        this.id = id;
        this.authorId = authorId;
        this.text = validateText(text);
        this.createdAt = createdAt;
        this.editedAt = editedAt;
    }

    // Publicar un aviso nuevo: siempre tiene autor
    public static Notice publish(UUID authorId, String text, Instant now) {
        if (authorId == null) {
            throw new IllegalArgumentException("Un aviso nuevo tiene que tener autor");
        }
        return new Notice(UUID.randomUUID(), authorId, text, now, null);
    }

    // Reconstruir un aviso leído de BD, también los de autores que ya no están
    public static Notice reconstitute(UUID id, UUID authorId, String text, Instant createdAt, Instant editedAt) {
        return new Notice(id, authorId, text, createdAt, editedAt);
    }

    private static String validateText(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("El aviso no puede estar vacío");
        }
        String trimmed = text.strip();
        if (trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("El aviso no puede superar los " + MAX_LENGTH + " caracteres");
        }
        return trimmed;
    }

    // Solo su autor y la administración pueden modificarlo o eliminarlo
    public boolean canBeManagedBy(UUID employeeId, boolean isAdministrator) {
        return isAdministrator || (authorId != null && authorId.equals(employeeId));
    }

    public void edit(String newText, Instant now) {
        String validated = validateText(newText);
        if (validated.equals(text)) {
            return; // Guardar sin cambios no lo marca como modificado
        }
        this.text = validated;
        this.editedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public Optional<UUID> getAuthorId() {
        return Optional.ofNullable(authorId);
    }

    public String getText() {
        return text;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Optional<Instant> getEditedAt() {
        return Optional.ofNullable(editedAt);
    }
}
