package com.viller.gespatiens.patients.application;

import com.viller.gespatiens.patients.domain.Dni;
import com.viller.gespatiens.patients.domain.Patient;
import com.viller.gespatiens.patients.domain.PatientDetails;
import com.viller.gespatiens.patients.domain.PatientNotFoundException;
import com.viller.gespatiens.patients.domain.PatientStillActiveException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Test unitario puro: sin Spring ni BD, con un repositorio en memoria
class DeletePatientUseCaseTest {

    private static final LocalDate ENTRY = LocalDate.of(2026, 1, 10);

    private final InMemoryPatientRepository repository = new InMemoryPatientRepository();
    private final DeletePatientUseCase useCase = new DeletePatientUseCase(repository);

    private static PatientDetails details() {
        return new PatientDetails("Juan", "García López", new Dni("12345678Z"), LocalDate.of(1990, 5, 1),
                "C/ Mayor 1, Valencia", "600111222", null, null, null, null, null);
    }

    @Test
    void deletesFormerPatient() {
        Patient former = repository.save(Patient.reconstitute(UUID.randomUUID(), details(), null,
                ENTRY, LocalDate.of(2026, 6, 30)));

        useCase.execute(former.getId());

        assertTrue(repository.findById(former.getId()).isEmpty());
    }

    @Test
    void rejectsDeletingActivePatient() {
        Patient active = repository.save(Patient.admit(details(), 1, ENTRY));

        assertThrows(PatientStillActiveException.class, () -> useCase.execute(active.getId()));
        assertTrue(repository.findById(active.getId()).isPresent());
    }

    @Test
    void rejectsUnknownPatient() {
        assertThrows(PatientNotFoundException.class, () -> useCase.execute(UUID.randomUUID()));
    }
}
