package com.viller.gespatiens.patients.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// La derivación de un residente (memoria final, 5.2): la institución que lo deriva, su profesional de
// referencia y, si el ingreso es por medida judicial, el juzgado y hasta cuándo dura la medida. Todo opcional:
// no todos los residentes llegan derivados
public class Referral {

    public static final int MAX_NOTES_LENGTH = 2000;

    private final UUID patientId;
    private String referringInstitution;
    private String contactName;
    private String contactPhone;
    private boolean courtOrdered;
    private String court;
    private LocalDate measureEndDate;
    private String notes;
    private Instant updatedAt;

    private Referral(UUID patientId, String referringInstitution, String contactName, String contactPhone,
                     boolean courtOrdered, String court, LocalDate measureEndDate, String notes, Instant updatedAt) {
        if (patientId == null || updatedAt == null) {
            throw new IllegalArgumentException("La derivación necesita residente y fecha");
        }
        this.patientId = patientId;
        describe(referringInstitution, contactName, contactPhone, courtOrdered, court, measureEndDate, notes);
        this.updatedAt = updatedAt;
    }

    // Lo que hay antes de que nadie la rellene
    public static Referral none(UUID patientId, Instant now) {
        return new Referral(patientId, null, null, null, false, null, null, null, now);
    }

    public static Referral reconstitute(UUID patientId, String referringInstitution, String contactName,
                                        String contactPhone, boolean courtOrdered, String court,
                                        LocalDate measureEndDate, String notes, Instant updatedAt) {
        return new Referral(patientId, referringInstitution, contactName, contactPhone, courtOrdered, court,
                measureEndDate, notes, updatedAt);
    }

    public void update(String referringInstitution, String contactName, String contactPhone, boolean courtOrdered,
                       String court, LocalDate measureEndDate, String notes, Instant now) {
        describe(referringInstitution, contactName, contactPhone, courtOrdered, court, measureEndDate, notes);
        this.updatedAt = now;
    }

    private void describe(String referringInstitution, String contactName, String contactPhone,
                          boolean courtOrdered, String court, LocalDate measureEndDate, String notes) {
        this.referringInstitution = optional(referringInstitution, "La institución que lo deriva", 150);
        this.contactName = optional(contactName, "El profesional de referencia", 100);
        this.contactPhone = optional(contactPhone, "El teléfono del profesional de referencia", 20);
        this.courtOrdered = courtOrdered;
        // Sin medida judicial, el juzgado y la fecha se descartan: no quedan datos que no se ven
        this.court = courtOrdered ? optional(court, "El juzgado", 150) : null;
        this.measureEndDate = courtOrdered ? measureEndDate : null;
        this.notes = optional(notes, "La información de la institución", MAX_NOTES_LENGTH);
    }

    private static String optional(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.strip();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(field + " no puede superar los " + maxLength + " caracteres");
        }
        return trimmed;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public String getReferringInstitution() {
        return referringInstitution;
    }

    public String getContactName() {
        return contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public boolean isCourtOrdered() {
        return courtOrdered;
    }

    public String getCourt() {
        return court;
    }

    public LocalDate getMeasureEndDate() {
        return measureEndDate;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
