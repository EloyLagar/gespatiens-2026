package com.viller.gespatiens.diary.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

// Un turno de un día en el diario: quién trabajó y la información de interés.
// La memoria dice que el empleado "podrá modificar toda la información o añadir más": cualquiera lo edita
public class ShiftLog {

    public static final int MAX_NOTES_LENGTH = 2000;

    private final UUID id;
    private final LocalDate date;
    private final DayPart part;
    private Set<UUID> staffIds;
    private String notes;
    // null si quien lo rellenó ya no está en la plantilla
    private UUID updatedBy;
    private Instant updatedAt;
    // Bloqueo optimista. null: todavía no se ha guardado nunca
    private final Long revision;

    private ShiftLog(UUID id, LocalDate date, DayPart part, Set<UUID> staffIds, String notes,
                     UUID updatedBy, Instant updatedAt, Long revision) {
        if (id == null || date == null || part == null || updatedAt == null) {
            throw new IllegalArgumentException("El turno necesita id, fecha, parte del día y fecha de modificación");
        }
        this.id = id;
        this.date = date;
        this.part = part;
        this.staffIds = Set.copyOf(staffIds == null ? Set.of() : staffIds);
        this.notes = validateNotes(notes);
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
        this.revision = revision;
    }

    // El primer registro de un turno: se crea cuando alguien lo rellena por primera vez
    public static ShiftLog record(LocalDate date, DayPart part, Set<UUID> staffIds, String notes,
                                  UUID editorId, Instant now) {
        return new ShiftLog(UUID.randomUUID(), date, part, staffIds, notes, editorId, now, null);
    }

    public static ShiftLog reconstitute(UUID id, LocalDate date, DayPart part, Set<UUID> staffIds, String notes,
                                        UUID updatedBy, Instant updatedAt, Long revision) {
        return new ShiftLog(id, date, part, staffIds, notes, updatedBy, updatedAt, revision);
    }

    public void update(Set<UUID> newStaffIds, String newNotes, UUID editorId, Instant now) {
        this.staffIds = Set.copyOf(newStaffIds == null ? Set.of() : newStaffIds);
        this.notes = validateNotes(newNotes);
        this.updatedBy = editorId;
        this.updatedAt = now;
    }

    private static String validateNotes(String notes) {
        if (notes == null || notes.isBlank()) {
            return null;
        }
        String trimmed = notes.strip();
        if (trimmed.length() > MAX_NOTES_LENGTH) {
            throw new IllegalArgumentException(
                    "La información del turno no puede superar los " + MAX_NOTES_LENGTH + " caracteres");
        }
        return trimmed;
    }

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

    public Optional<String> getNotes() {
        return Optional.ofNullable(notes);
    }

    public Optional<UUID> getUpdatedBy() {
        return Optional.ofNullable(updatedBy);
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // La versión que ve la pantalla: 0 si todavía no se ha guardado nunca y, después, una más que la revisión de
    // la persistencia (que empieza en 0 al crearlo). Así quien vio el turno vacío no pisa al que lo acaba de crear
    public long version() {
        return revision == null ? 0 : revision + 1;
    }

    // RF8: quien guarda dice qué versión tenía delante. Si otra persona lo cambió mientras tanto, no se pisa.
    // null: quien llama no lo comprueba (los tests y las llamadas antiguas a la API)
    public void checkVersion(Long expected) {
        if (expected != null && expected != version()) {
            throw new DiaryConflictException("el turno");
        }
    }

    public Optional<Long> getRevision() {
        return Optional.ofNullable(revision);
    }
}
