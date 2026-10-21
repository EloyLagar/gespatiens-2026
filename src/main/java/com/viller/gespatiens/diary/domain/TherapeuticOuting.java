package com.viller.gespatiens.diary.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

// Una salida terapéutica de un residente. La memoria: "Otro campo deberá responder a las salidas
// terapéuticas y a los regresos de estas permitiendo introducir la hora"
public class TherapeuticOuting {

    public static final int MAX_NOTES_LENGTH = 200;

    private final UUID id;
    private final LocalDate date;
    private final UUID patientId;
    private LocalTime leftAt;
    // null mientras no ha vuelto
    private LocalTime returnedAt;
    private String notes;

    private TherapeuticOuting(UUID id, LocalDate date, UUID patientId, LocalTime leftAt, LocalTime returnedAt,
                              String notes) {
        if (id == null || date == null || patientId == null) {
            throw new IllegalArgumentException("La salida necesita id, fecha y residente");
        }
        this.id = id;
        this.date = date;
        this.patientId = patientId;
        setTimes(leftAt, returnedAt);
        this.notes = validateNotes(notes);
    }

    public static TherapeuticOuting leave(LocalDate date, UUID patientId, LocalTime leftAt, String notes) {
        return new TherapeuticOuting(UUID.randomUUID(), date, patientId, leftAt, null, notes);
    }

    public static TherapeuticOuting reconstitute(UUID id, LocalDate date, UUID patientId, LocalTime leftAt,
                                                 LocalTime returnedAt, String notes) {
        return new TherapeuticOuting(id, date, patientId, leftAt, returnedAt, notes);
    }

    // Corregir la salida o anotar el regreso
    public void update(LocalTime newLeftAt, LocalTime newReturnedAt, String newNotes) {
        setTimes(newLeftAt, newReturnedAt);
        this.notes = validateNotes(newNotes);
    }

    public boolean hasReturned() {
        return returnedAt != null;
    }

    private void setTimes(LocalTime leftAt, LocalTime returnedAt) {
        if (leftAt == null) {
            throw new IllegalArgumentException("Indica la hora de salida");
        }
        if (returnedAt != null && returnedAt.isBefore(leftAt)) {
            throw new IllegalArgumentException("La hora de regreso no puede ser anterior a la de salida");
        }
        this.leftAt = leftAt;
        this.returnedAt = returnedAt;
    }

    private static String validateNotes(String notes) {
        if (notes == null || notes.isBlank()) {
            return null;
        }
        String trimmed = notes.strip();
        if (trimmed.length() > MAX_NOTES_LENGTH) {
            throw new IllegalArgumentException("La nota no puede superar los " + MAX_NOTES_LENGTH + " caracteres");
        }
        return trimmed;
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

    public LocalTime getLeftAt() {
        return leftAt;
    }

    public Optional<LocalTime> getReturnedAt() {
        return Optional.ofNullable(returnedAt);
    }

    public Optional<String> getNotes() {
        return Optional.ofNullable(notes);
    }
}
