package com.viller.gespatiens.diary.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiaryDomainTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 20);
    private static final Instant NOW = Instant.parse("2026-10-20T15:00:00Z");
    private final UUID editor = UUID.randomUUID();

    @Test
    void shiftKeepsStaffAndTrimmedNotes() {
        UUID educator = UUID.randomUUID();
        ShiftLog log = ShiftLog.record(DAY, DayPart.MORNING, Set.of(educator), "  Todo tranquilo  ", editor, NOW);

        assertEquals(Set.of(educator), log.getStaffIds());
        assertEquals("Todo tranquilo", log.getNotes().orElseThrow());
        assertEquals(editor, log.getUpdatedBy().orElseThrow());
    }

    @Test
    void blankShiftNotesAreStoredAsNothing() {
        ShiftLog log = ShiftLog.record(DAY, DayPart.NIGHT, Set.of(), "   ", editor, NOW);

        assertTrue(log.getNotes().isEmpty());
    }

    @Test
    void shiftNotesHaveALimit() {
        assertThrows(IllegalArgumentException.class, () ->
                ShiftLog.record(DAY, DayPart.AFTERNOON, Set.of(), "x".repeat(2001), editor, NOW));
    }

    @Test
    void reductionNeedsAtLeastOneType() {
        UUID patient = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () -> Reduction.record(DAY, patient, " ", false, false, editor, NOW));
        assertDoesNotThrow(() -> Reduction.record(DAY, patient, null, false, true, editor, NOW));
        assertDoesNotThrow(() -> Reduction.record(DAY, patient, "No puede cargar peso", false, false, editor, NOW));
    }

    @Test
    void changingAReductionKeepsTheInvariant() {
        Reduction reduction = Reduction.record(DAY, UUID.randomUUID(), null, true, false, editor, NOW);

        assertThrows(IllegalArgumentException.class, () -> reduction.change(null, false, false, editor, NOW));
        assertTrue(reduction.isTotal());
    }
}
