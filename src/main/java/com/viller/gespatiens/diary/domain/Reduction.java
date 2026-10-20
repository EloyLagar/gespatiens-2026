package com.viller.gespatiens.diary.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

// Un residente rebajado un día: tiene permiso para no hacer ciertas actividades.
// Tipos de la memoria (tabla Reductions): parcial (de qué, en texto), total y deportiva
public class Reduction {

    public static final int MAX_PARTIAL_LENGTH = 100;

    private final UUID id;
    private final LocalDate date;
    private final UUID patientId;
    private String partial;
    private boolean total;
    private boolean sport;
    private UUID recordedBy;
    private Instant recordedAt;

    private Reduction(UUID id, LocalDate date, UUID patientId, String partial, boolean total, boolean sport,
                      UUID recordedBy, Instant recordedAt) {
        if (id == null || date == null || patientId == null || recordedAt == null) {
            throw new IllegalArgumentException("La rebaja necesita id, fecha, residente y fecha de registro");
        }
        this.id = id;
        this.date = date;
        this.patientId = patientId;
        setTypes(partial, total, sport);
        this.recordedBy = recordedBy;
        this.recordedAt = recordedAt;
    }

    public static Reduction record(LocalDate date, UUID patientId, String partial, boolean total, boolean sport,
                                   UUID recorderId, Instant now) {
        return new Reduction(UUID.randomUUID(), date, patientId, partial, total, sport, recorderId, now);
    }

    public static Reduction reconstitute(UUID id, LocalDate date, UUID patientId, String partial, boolean total,
                                         boolean sport, UUID recordedBy, Instant recordedAt) {
        return new Reduction(id, date, patientId, partial, total, sport, recordedBy, recordedAt);
    }

    public void change(String newPartial, boolean newTotal, boolean newSport, UUID recorderId, Instant now) {
        setTypes(newPartial, newTotal, newSport);
        this.recordedBy = recorderId;
        this.recordedAt = now;
    }

    private void setTypes(String partial, boolean total, boolean sport) {
        String cleaned = partial == null || partial.isBlank() ? null : partial.strip();
        if (cleaned != null && cleaned.length() > MAX_PARTIAL_LENGTH) {
            throw new IllegalArgumentException(
                    "La rebaja parcial no puede superar los " + MAX_PARTIAL_LENGTH + " caracteres");
        }
        if (cleaned == null && !total && !sport) {
            throw new IllegalArgumentException("Indica el tipo de rebaja: parcial, total o deportiva");
        }
        this.partial = cleaned;
        this.total = total;
        this.sport = sport;
    }

    public UUID getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public Optional<String> getPartial() {
        return Optional.ofNullable(partial);
    }

    public boolean isTotal() {
        return total;
    }

    public boolean isSport() {
        return sport;
    }

    public Optional<UUID> getRecordedBy() {
        return Optional.ofNullable(recordedBy);
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
