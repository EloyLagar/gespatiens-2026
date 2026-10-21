package com.viller.gespatiens.diary.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "reductions")
public class ReductionJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private UUID patientId;

    @Column(name = "partial_reduction", length = 100)
    private String partial;

    @Column(name = "total_reduction", nullable = false)
    private boolean total;

    @Column(name = "sport_reduction", nullable = false)
    private boolean sport;

    private UUID recordedBy;

    @Column(nullable = false)
    private Instant recordedAt;

    public UUID getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public String getPartial() {
        return partial;
    }

    public boolean isTotal() {
        return total;
    }

    public boolean isSport() {
        return sport;
    }

    public UUID getRecordedBy() {
        return recordedBy;
    }

    public Instant getRecordedAt() {
        return recordedAt;
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

    public void setPartial(String partial) {
        this.partial = partial;
    }

    public void setTotal(boolean total) {
        this.total = total;
    }

    public void setSport(boolean sport) {
        this.sport = sport;
    }

    public void setRecordedBy(UUID recordedBy) {
        this.recordedBy = recordedBy;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }
}
