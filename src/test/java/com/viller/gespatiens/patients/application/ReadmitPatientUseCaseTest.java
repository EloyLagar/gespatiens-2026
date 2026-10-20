package com.viller.gespatiens.patients.application;

import com.viller.gespatiens.patients.domain.CenterFullException;
import com.viller.gespatiens.patients.domain.Dni;
import com.viller.gespatiens.patients.domain.Patient;
import com.viller.gespatiens.patients.domain.PatientAlreadyActiveException;
import com.viller.gespatiens.patients.domain.PatientDetails;
import com.viller.gespatiens.patients.domain.PatientNotFoundException;
import com.viller.gespatiens.patients.domain.ResidentNumberAssigner;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Test unitario puro: sin Spring ni BD, con un repositorio en memoria
class ReadmitPatientUseCaseTest {

    private static final LocalDate ENTRY = LocalDate.of(2026, 1, 10);
    private static final LocalDate EXIT = LocalDate.of(2026, 6, 30);
    private static final LocalDate REENTRY = LocalDate.of(2026, 9, 1);

    private final InMemoryPatientRepository repository = new InMemoryPatientRepository();
    private final ResidentNumberAssigner assigner = new ResidentNumberAssigner();
    private final ReadmitPatientUseCase useCase = new ReadmitPatientUseCase(repository, assigner);
    private final AdmitPatientUseCase admit = new AdmitPatientUseCase(repository, assigner);

    // DNIs válidos generados con la letra de control correcta: 00000000T, 00000001R, ...
    private static PatientDetails detailsWithDni(int dniNumber) {
        String letters = "TRWAGMYFPDXBNJZSQVHLCKE";
        String dni = String.format("%08d", dniNumber) + letters.charAt(dniNumber % 23);
        return new PatientDetails("Residente " + dniNumber, "Apellido", new Dni(dni), LocalDate.of(1990, 1, 1),
                "C/ Mayor 1", "600000000", null, null, null, null, null);
    }

    private Patient formerPatient(int dniNumber) {
        return repository.save(Patient.reconstitute(UUID.randomUUID(), detailsWithDni(dniNumber), null, ENTRY, EXIT));
    }

    @Test
    void readmittedPatientGetsLowestFreeNumber() {
        admit.execute(detailsWithDni(1), ENTRY);
        admit.execute(detailsWithDni(2), ENTRY);
        Patient former = formerPatient(3);

        Patient readmitted = useCase.execute(former.getId(), REENTRY);

        assertTrue(readmitted.isActive());
        assertEquals(3, readmitted.getNumber());
        assertEquals(REENTRY, readmitted.getEntryDate());
        assertEquals(readmitted.getNumber(), repository.findById(former.getId()).orElseThrow().getNumber());
    }

    @Test
    void rejectsReadmissionWhenCenterIsFull() {
        for (int i = 1; i <= 32; i++) {
            admit.execute(detailsWithDni(i), ENTRY);
        }
        Patient former = formerPatient(33);

        assertThrows(CenterFullException.class, () -> useCase.execute(former.getId(), REENTRY));
    }

    @Test
    void activePatientGetsAlreadyActiveEvenIfCenterIsFull() {
        for (int i = 1; i <= 32; i++) {
            admit.execute(detailsWithDni(i), ENTRY);
        }
        Patient active = repository.findByNumber(1);

        assertThrows(PatientAlreadyActiveException.class, () -> useCase.execute(active.getId(), REENTRY));
    }

    @Test
    void rejectsUnknownPatient() {
        assertThrows(PatientNotFoundException.class, () -> useCase.execute(UUID.randomUUID(), REENTRY));
    }
}
