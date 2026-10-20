package com.viller.gespatiens.interventions.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterventionDomainTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 21);
    private static final Instant NOW = Instant.parse("2026-10-21T10:00:00Z");
    private final UUID patient = UUID.randomUUID();
    private final UUID author = UUID.randomUUID();

    @Test
    void interventionKeepsTrimmedTextAndNeedsAuthorDateAndText() {
        Intervention intervention = Intervention.record(patient, author, TODAY, "  Entrevista con la familia ", NOW, TODAY);

        assertEquals("Entrevista con la familia", intervention.getText());
        assertEquals(author, intervention.getAuthorId().orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> Intervention.record(patient, null, TODAY, "x", NOW, TODAY));
        assertThrows(IllegalArgumentException.class, () -> Intervention.record(patient, author, null, "x", NOW, TODAY));
        assertThrows(IllegalArgumentException.class, () -> Intervention.record(patient, author, TODAY, " ", NOW, TODAY));
        assertThrows(IllegalArgumentException.class, () ->
                Intervention.record(patient, author, TODAY, "a".repeat(Intervention.MAX_LENGTH + 1), NOW, TODAY));
    }

    @Test
    void interventionCannotBeOfADayThatHasNotCome() {
        assertThrows(IllegalArgumentException.class, () ->
                Intervention.record(patient, author, TODAY.plusDays(1), "Tutoría", NOW, TODAY));
        Intervention intervention = Intervention.record(patient, author, TODAY.minusDays(3), "Tutoría", NOW, TODAY);
        assertThrows(IllegalArgumentException.class, () ->
                intervention.edit(TODAY.plusDays(1), "Tutoría", NOW, TODAY));
    }

    @Test
    void editingWithoutChangesDoesNotMarkItAsEdited() {
        Intervention intervention = Intervention.record(patient, author, TODAY, "Tutoría", NOW, TODAY);

        intervention.edit(TODAY, " Tutoría ", NOW.plusSeconds(60), TODAY);
        assertTrue(intervention.getEditedAt().isEmpty());
        intervention.edit(TODAY.minusDays(1), "Tutoría", NOW.plusSeconds(60), TODAY);
        assertEquals(NOW.plusSeconds(60), intervention.getEditedAt().orElseThrow());
    }

    @Test
    void onlyTheAuthorOrAdministrationManageIt() {
        Intervention intervention = Intervention.record(patient, author, TODAY, "Tutoría", NOW, TODAY);
        Intervention orphan = Intervention.reconstitute(UUID.randomUUID(), patient, null, TODAY, "Tutoría", NOW, null);

        assertTrue(intervention.canBeManagedBy(author, false));
        assertFalse(intervention.canBeManagedBy(UUID.randomUUID(), false));
        assertTrue(intervention.canBeManagedBy(UUID.randomUUID(), true));
        assertFalse(orphan.canBeManagedBy(author, false));
    }

    @Test
    void tutorsAreReplacedAsAWhole() {
        UUID ana = UUID.randomUUID();
        UUID pablo = UUID.randomUUID();
        ResidentTutors tutors = ResidentTutors.none(patient);

        tutors.assign(Set.of(ana, pablo));
        assertTrue(tutors.isTutor(ana));
        tutors.assign(Set.of(pablo));
        assertFalse(tutors.isTutor(ana));
        tutors.assign(null);
        assertTrue(tutors.getTutorIds().isEmpty());
    }
}
