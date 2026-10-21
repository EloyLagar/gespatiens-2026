package com.viller.gespatiens.patients.application;

import com.viller.gespatiens.patients.domain.StayEvents;
import com.viller.gespatiens.patients.domain.Dni;
import com.viller.gespatiens.patients.domain.Patient;
import com.viller.gespatiens.patients.domain.PatientAlreadyDischargedException;
import com.viller.gespatiens.patients.domain.PatientDetails;
import com.viller.gespatiens.patients.domain.PatientNotFoundException;
import com.viller.gespatiens.patients.domain.ResidentNumberAssigner;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Test unitario puro: sin Spring ni BD, con un repositorio en memoria
class DischargePatientUseCaseTest {

    private static final LocalDate ENTRY = LocalDate.of(2026, 1, 10);
    private static final LocalDate EXIT = LocalDate.of(2026, 6, 30);

    private final InMemoryPatientRepository repository = new InMemoryPatientRepository();
    private final DischargePatientUseCase useCase = new DischargePatientUseCase(repository, StayEvents.none());

    private Patient admittedPatient() {
        PatientDetails details = new PatientDetails("Juan", "García López", new Dni("12345678Z"),
                LocalDate.of(1990, 5, 1), "C/ Mayor 1, Valencia", "600111222", null, null, null, null, null);
        return repository.save(Patient.admit(details, 3, ENTRY));
    }

    @Test
    void dischargedPatientKeepsDataAndFreesNumber() {
        Patient patient = admittedPatient();

        useCase.execute(patient.getId(), EXIT);

        // RF9: el residente sigue en el repositorio, ahora como antiguo
        Patient stored = repository.findById(patient.getId()).orElseThrow();
        assertFalse(stored.isActive());
        assertNull(stored.getNumber());
        assertEquals(EXIT, stored.getExitDate());
        assertEquals("Juan", stored.getDetails().name());
        assertTrue(repository.findOccupiedNumbers().isEmpty());
    }

    @Test
    void freedNumberIsReusedByTheNextAdmission() {
        Patient patient = admittedPatient();
        useCase.execute(patient.getId(), EXIT);

        AdmitPatientUseCase admit = new AdmitPatientUseCase(repository, new ResidentNumberAssigner(), StayEvents.none());
        PatientDetails other = new PatientDetails("Ana", "Pérez", new Dni("00000001R"),
                LocalDate.of(1985, 3, 3), "C/ Sol 2", "600333444", null, null, null, null, null);

        assertEquals(1, admit.execute(other, EXIT).getNumber());
    }

    @Test
    void rejectsUnknownPatient() {
        assertThrows(PatientNotFoundException.class, () -> useCase.execute(UUID.randomUUID(), EXIT));
    }

    @Test
    void rejectsDischargingTwice() {
        Patient patient = admittedPatient();
        useCase.execute(patient.getId(), EXIT);

        assertThrows(PatientAlreadyDischargedException.class, () -> useCase.execute(patient.getId(), EXIT));
    }
}
