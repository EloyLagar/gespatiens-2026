package com.viller.gespatiens.patients.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void dischargeFreesNumberAndSetsExitDate() {
        Patient patient = Patient.admit(details(), 5, LocalDate.of(2026, 1, 1));

        patient.discharge(LocalDate.of(2026, 6, 1));

        assertFalse(patient.isActive());
        assertNull(patient.getNumber());
        assertEquals(LocalDate.of(2026, 6, 1), patient.getExitDate());
    }

    @Test
    void dischargeAllowsSameDayAsEntry() {
        Patient patient = Patient.admit(details(), 5, LocalDate.of(2026, 1, 1));

        assertDoesNotThrow(() -> patient.discharge(LocalDate.of(2026, 1, 1)));
    }

    @Test
    void dischargeRejectsInvalidExitDate() {
        Patient patient = Patient.admit(details(), 5, LocalDate.of(2026, 1, 1));

        assertThrows(IllegalArgumentException.class, () -> patient.discharge(LocalDate.of(2025, 12, 31)));
        assertThrows(IllegalArgumentException.class, () -> patient.discharge(LocalDate.now().plusDays(1)));
        assertThrows(IllegalArgumentException.class, () -> patient.discharge(null));
        // Un intento fallido no deja al residente a medio dar de baja
        assertTrue(patient.isActive());
        assertEquals(5, patient.getNumber());
    }

    @Test
    void cannotDischargeTwice() {
        Patient patient = Patient.reconstitute(UUID.randomUUID(), details(), null,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 1));

        assertThrows(PatientAlreadyDischargedException.class, () -> patient.discharge(LocalDate.of(2026, 7, 1)));
    }

    private static Patient formerPatient() {
        return Patient.reconstitute(UUID.randomUUID(), details(), null,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 1));
    }

    @Test
    void readmitGivesNewNumberAndStartsNewStay() {
        Patient patient = formerPatient();

        patient.readmit(7, LocalDate.of(2026, 9, 1));

        assertTrue(patient.isActive());
        assertEquals(7, patient.getNumber());
        assertEquals(LocalDate.of(2026, 9, 1), patient.getEntryDate());
        assertNull(patient.getExitDate());
        assertEquals("Juan", patient.getDetails().name());
    }

    @Test
    void readmitRejectsEntryBeforeLastExit() {
        Patient patient = formerPatient();

        assertThrows(IllegalArgumentException.class, () -> patient.readmit(7, LocalDate.of(2026, 5, 31)));
        assertThrows(IllegalArgumentException.class, () -> patient.readmit(7, null));
        assertThrows(IllegalArgumentException.class, () -> patient.readmit(33, LocalDate.of(2026, 9, 1)));
        // Un intento fallido no deja al residente a medio reincorporar
        assertFalse(patient.isActive());
        assertNull(patient.getNumber());
    }

    @Test
    void readmitAllowsSameDayAsLastExit() {
        assertDoesNotThrow(() -> formerPatient().readmit(7, LocalDate.of(2026, 6, 1)));
    }

    @Test
    void cannotReadmitActivePatient() {
        Patient patient = Patient.admit(details(), 5, LocalDate.of(2026, 1, 1));

        assertThrows(PatientAlreadyActiveException.class, () -> patient.readmit(6, LocalDate.of(2026, 2, 1)));
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
