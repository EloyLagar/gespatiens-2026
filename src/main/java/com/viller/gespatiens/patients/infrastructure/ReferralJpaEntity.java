package com.viller.gespatiens.patients.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "patient_referrals")
public class ReferralJpaEntity {

    // Una derivación por residente: su id es el del residente
    @Id
    private UUID patientId;

    @Column(length = 150)
    private String referringInstitution;

    @Column(length = 100)
    private String contactName;

    @Column(length = 20)
    private String contactPhone;

    @Column(nullable = false)
    private boolean courtOrdered;

    @Column(length = 150)
    private String court;

    private LocalDate measureEndDate;

    @Column(length = 2000)
    private String notes;

    @Column(nullable = false)
    private Instant updatedAt;

    public UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
    }

    public String getReferringInstitution() {
        return referringInstitution;
    }

    public void setReferringInstitution(String referringInstitution) {
        this.referringInstitution = referringInstitution;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public boolean isCourtOrdered() {
        return courtOrdered;
    }

    public void setCourtOrdered(boolean courtOrdered) {
        this.courtOrdered = courtOrdered;
    }

    public String getCourt() {
        return court;
    }

    public void setCourt(String court) {
        this.court = court;
    }

    public LocalDate getMeasureEndDate() {
        return measureEndDate;
    }

    public void setMeasureEndDate(LocalDate measureEndDate) {
        this.measureEndDate = measureEndDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
