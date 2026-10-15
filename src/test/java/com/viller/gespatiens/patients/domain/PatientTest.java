package com.viller.gespatiens.patients.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatientTest {

    static PatientDetails details() {
        return new PatientDetails("Juan", "García López", new Dni("12345678Z"), LocalDate.of(1990, 5, 1),
                "C/ Mayor 1, Valencia", "600111222", null, null, null, null, null);
    }

    @Test
    void admittedPatientIsActiveWithNumber() {
        Patient patient = Patient.admit(details(), 5, LocalDate.of(2026, 10, 1));

        assertNotNull(patient.getId());
        assertTrue(patient.isActive());
        assertEquals(5, patient.getNumber());
    }

    @Test
    void rejectsNumberOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> Patient.admit(details(), 0, LocalDate.now()));
        assertThrows(IllegalArgumentException.class, () -> Patient.admit(details(), 33, LocalDate.now()));
    }

    @Test
    void dischargedPatientCannotKeepNumber() {
        LocalDate entry = LocalDate.of(2026, 1, 1);
        LocalDate exit = LocalDate.of(2026, 6, 1);

        assertThrows(IllegalArgumentException.class, () ->
                Patient.reconstitute(UUID.randomUUID(), details(), 5, entry, exit));
        assertDoesNotThrow(() -> Patient.reconstitute(UUID.randomUUID(), details(), null, entry, exit));
    }

    @Test
    void exitDateCannotBeBeforeEntryDate() {
        assertThrows(IllegalArgumentException.class, () -> Patient.reconstitute(
                UUID.randomUUID(), details(), null, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 1, 1)));
    }

    @Test
    void detailsRejectFutureBirthDateAndMissingFields() {
        assertThrows(IllegalArgumentException.class, () -> new PatientDetails("Juan", "García",
                new Dni("12345678Z"), LocalDate.now().plusDays(1), "C/ Mayor 1", "600111222",
                null, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new PatientDetails(" ", "García",
                new Dni("12345678Z"), LocalDate.of(1990, 5, 1), "C/ Mayor 1", "600111222",
                null, null, null, null, null));
    }
}
