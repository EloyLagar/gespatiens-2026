package com.viller.gespatiens.interventions.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "interventions")
public class InterventionJpaEntity {

    // El id lo genera el dominio (UUID), no la base de datos: sin @GeneratedValue
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID patientId;

    // Solo el id: las intervenciones no cargan la entidad del empleado (otro módulo)
    private UUID authorId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = 2000)
    private String text;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant editedAt;

    public UUID getId() {
        return id;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public UUID getAuthorId() {
        return authorId;
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

    public Instant getEditedAt() {
        return editedAt;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
    }

    public void setDate(LocalDate date) {
        this.date = date;
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
