package com.viller.gespatiens.diary.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "therapeutic_outings")
public class TherapeuticOutingJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private UUID patientId;

    @Column(nullable = false)
    private LocalTime leftAt;

    private LocalTime returnedAt;

    @Column(length = 200)
    private String notes;

    public UUID getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public LocalTime getLeftAt() {
        return leftAt;
    }

    public LocalTime getReturnedAt() {
        return returnedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
    }

    public void setLeftAt(LocalTime leftAt) {
        this.leftAt = leftAt;
    }

    public void setReturnedAt(LocalTime returnedAt) {
        this.returnedAt = returnedAt;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
