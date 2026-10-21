package com.viller.gespatiens.diary.infrastructure;

import com.viller.gespatiens.diary.domain.DayPart;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "shift_logs")
public class ShiftLogJpaEntity {

    @Id
    private UUID id;

    // Bloqueo optimista (RF8): dos guardados simultáneos no se pisan. null: todavía no se ha guardado
    @Version
    private Long revision;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_part", nullable = false, length = 10)
    private DayPart part;

    // Solo los ids: el diario no carga las entidades de otro módulo. EAGER porque el adaptador
    // traduce a dominio fuera de la transacción del repositorio y son pocos empleados por turno
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "shift_staff", joinColumns = @JoinColumn(name = "shift_log_id"))
    @Column(name = "user_id", nullable = false)
    private Set<UUID> staffIds = new HashSet<>();

    @Column(length = 2000)
    private String notes;

    private UUID updatedBy;

    @Column(nullable = false)
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public DayPart getPart() {
        return part;
    }

    public Set<UUID> getStaffIds() {
        return staffIds;
    }

    public String getNotes() {
        return notes;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setPart(DayPart part) {
        this.part = part;
    }

    public void setStaffIds(Set<UUID> staffIds) {
        this.staffIds = new HashSet<>(staffIds);
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getRevision() {
        return revision;
    }

    public void setRevision(Long revision) {
        this.revision = revision;
    }
}
