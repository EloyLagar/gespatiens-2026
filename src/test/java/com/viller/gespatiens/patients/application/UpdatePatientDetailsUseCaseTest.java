package com.viller.gespatiens.patients.application;

import com.viller.gespatiens.patients.domain.Dni;
import com.viller.gespatiens.patients.domain.Patient;
import com.viller.gespatiens.patients.domain.PatientAlreadyRegisteredException;
import com.viller.gespatiens.patients.domain.PatientDetails;
import com.viller.gespatiens.patients.domain.PatientNotFoundException;
import com.viller.gespatiens.patients.domain.VisitCodeAlreadyInUseException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Test unitario puro: sin Spring ni BD, con un repositorio en memoria
class UpdatePatientDetailsUseCaseTest {

    private static final LocalDate ENTRY = LocalDate.of(2026, 1, 10);

    private final InMemoryPatientRepository repository = new InMemoryPatientRepository();
    private final UpdatePatientDetailsUseCase useCase = new UpdatePatientDetailsUseCase(repository);

    private static PatientDetails details(String name, String dni, String visitCode) {
        return new PatientDetails(name, "García López", new Dni(dni), LocalDate.of(1990, 5, 1),
                "C/ Mayor 1, Valencia", "600111222", null, visitCode, null, null, null);
    }

    @Test
    void updatesDetailsKeepingNumberAndDates() {
        Patient patient = repository.save(Patient.admit(details("Juan", "12345678Z", "V-001"), 3, ENTRY));

        useCase.execute(patient.getId(), details("Juan Carlos", "12345678Z", "V-001"));

        Patient stored = repository.findById(patient.getId()).orElseThrow();
        assertEquals("Juan Carlos", stored.getDetails().name());
        assertEquals(3, stored.getNumber());
        assertEquals(ENTRY, stored.getEntryDate());
    }

    @Test
    void rejectsDniOfAnotherPatient() {
        repository.save(Patient.admit(details("Ana", "00000001R", null), 1, ENTRY));
        Patient juan = repository.save(Patient.admit(details("Juan", "12345678Z", null), 2, ENTRY));

        assertThrows(PatientAlreadyRegisteredException.class, () ->
                useCase.execute(juan.getId(), details("Juan", "00000001R", null)));
    }

    @Test
    void rejectsVisitCodeOfAnotherPatient() {
        repository.save(Patient.admit(details("Ana", "00000001R", "V-001"), 1, ENTRY));
        Patient juan = repository.save(Patient.admit(details("Juan", "12345678Z", null), 2, ENTRY));

        assertThrows(VisitCodeAlreadyInUseException.class, () ->
                useCase.execute(juan.getId(), details("Juan", "12345678Z", "V-001")));
    }

    @Test
    void rejectsUnknownPatient() {
        assertThrows(PatientNotFoundException.class, () ->
                useCase.execute(UUID.randomUUID(), details("Juan", "12345678Z", null)));
    }
}
