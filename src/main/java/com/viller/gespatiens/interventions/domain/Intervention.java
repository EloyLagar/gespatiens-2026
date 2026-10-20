package com.viller.gespatiens.interventions.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

// Una intervención con un residente (Interventions en la memoria): quién la hizo, cuándo y qué se hizo.
// La memoria: "para cada intervención se indicarán los tutores del usuario y la información de la
// intervención realizada", y "únicamente podrán generar los tutores de cada usuario"
public class Intervention {

    public static final int MAX_LENGTH = 2000;

    private final UUID id;
    private final UUID patientId;
    // null si el autor ya no está en la plantilla (la BD lo pone a null al borrar al empleado)
    private final UUID authorId;
    private LocalDate date;
    private String text;
    private final Instant createdAt;
    private Instant editedAt;

    private Intervention(UUID id, UUID patientId, UUID authorId, LocalDate date, String text, Instant createdAt,
                         Instant editedAt) {
        if (id == null || patientId == null) {
            throw new IllegalArgumentException("La intervención necesita id y residente");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("La fecha de registro de la intervención no puede estar vacía");
        }
        if (editedAt != null && editedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Una intervención no puede modificarse antes de registrarse");
        }
        if (date == null) {
            throw new IllegalArgumentException("Indica el día de la intervención");
        }
        this.id = id;
        this.patientId = patientId;
        this.authorId = authorId;
        this.date = date;
        this.text = validateText(text);
        this.createdAt = createdAt;
        this.editedAt = editedAt;
    }

    // Registrar una intervención nueva: siempre tiene autor y no puede ser de un día que no ha llegado
    public static Intervention record(UUID patientId, UUID authorId, LocalDate date, String text, Instant now,
                                      LocalDate today) {
        if (authorId == null) {
            throw new IllegalArgumentException("Una intervención nueva tiene que tener autor");
        }
        checkNotFuture(date, today);
        return new Intervention(UUID.randomUUID(), patientId, authorId, date, text, now, null);
    }

    public static Intervention reconstitute(UUID id, UUID patientId, UUID authorId, LocalDate date, String text,
                                            Instant createdAt, Instant editedAt) {
        return new Intervention(id, patientId, authorId, date, text, createdAt, editedAt);
    }

    // Solo su autor y la administración pueden modificarla o eliminarla: cambiarla sería cambiar lo que firmó
    public boolean canBeManagedBy(UUID employeeId, boolean isAdministrator) {
        return isAdministrator || (authorId != null && authorId.equals(employeeId));
    }

    public void edit(LocalDate newDate, String newText, Instant now, LocalDate today) {
        if (newDate == null) {
            throw new IllegalArgumentException("Indica el día de la intervención");
        }
        checkNotFuture(newDate, today);
        String validated = validateText(newText);
        if (newDate.equals(date) && validated.equals(text)) {
            return; // Guardar sin cambios no la marca como modificada
        }
        this.date = newDate;
        this.text = validated;
        this.editedAt = now;
    }

    private static void checkNotFuture(LocalDate date, LocalDate today) {
        if (date != null && date.isAfter(today)) {
            throw new IllegalArgumentException("No se puede registrar una intervención de un día que no ha llegado");
        }
    }

    private static String validateText(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Describe la intervención");
        }
        String trimmed = text.strip();
        if (trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("La intervención no puede superar los " + MAX_LENGTH + " caracteres");
        }
        return trimmed;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public Optional<UUID> getAuthorId() {
        return Optional.ofNullable(authorId);
    }

    public LocalDate getDate() {
        return date;
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
