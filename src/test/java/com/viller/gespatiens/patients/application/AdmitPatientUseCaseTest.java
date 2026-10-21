package com.viller.gespatiens.patients.application;

import com.viller.gespatiens.patients.domain.StayEvents;
import com.viller.gespatiens.patients.domain.CenterFullException;
import com.viller.gespatiens.patients.domain.Dni;
import com.viller.gespatiens.patients.domain.Patient;
import com.viller.gespatiens.patients.domain.PatientAlreadyRegisteredException;
import com.viller.gespatiens.patients.domain.PatientDetails;
import com.viller.gespatiens.patients.domain.ResidentNumberAssigner;
import com.viller.gespatiens.patients.domain.VisitCodeAlreadyInUseException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Test unitario puro: sin Spring ni BD, con un repositorio en memoria
class AdmitPatientUseCaseTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);

    private final InMemoryPatientRepository repository = new InMemoryPatientRepository();
    private final AdmitPatientUseCase useCase = new AdmitPatientUseCase(repository, new ResidentNumberAssigner(), StayEvents.none());

    // DNIs válidos generados con la letra de control correcta: 00000000T, 00000001R, ...
    private static PatientDetails detailsWithDni(int dniNumber) {
        String letters = "TRWAGMYFPDXBNJZSQVHLCKE";
        String dni = String.format("%08d", dniNumber) + letters.charAt(dniNumber % 23);
        return new PatientDetails("Residente " + dniNumber, "Apellido", new Dni(dni), LocalDate.of(1990, 1, 1),
                "C/ Mayor 1", "600000000", null, null, null, null, null);
    }

    @Test
    void firstPatientGetsNumberOne() {
        Patient patient = useCase.execute(detailsWithDni(1), TODAY);

        assertEquals(1, patient.getNumber());
        assertEquals(TODAY, patient.getEntryDate());
    }

    @Test
    void reusesNumberFreedByADischargedPatient() {
        useCase.execute(detailsWithDni(1), TODAY);
        useCase.execute(detailsWithDni(2), TODAY);
        useCase.execute(detailsWithDni(3), TODAY);
        Patient second = repository.findByNumber(2);
        second.discharge(TODAY);
        repository.save(second);

        assertEquals(2, useCase.execute(detailsWithDni(4), TODAY).getNumber());
    }

    @Test
    void rejectsDuplicateDni() {
        useCase.execute(detailsWithDni(1), TODAY);

        assertThrows(PatientAlreadyRegisteredException.class, () -> useCase.execute(detailsWithDni(1), TODAY));
    }

    @Test
    void rejectsDuplicateVisitCode() {
        useCase.execute(withVisitCode(detailsWithDni(1), "V-001"), TODAY);

        assertThrows(VisitCodeAlreadyInUseException.class, () ->
                useCase.execute(withVisitCode(detailsWithDni(2), "V-001"), TODAY));
    }

    @Test
    void patientsWithoutVisitCodeDoNotCollide() {
        useCase.execute(withVisitCode(detailsWithDni(1), ""), TODAY);

        assertEquals(2, useCase.execute(withVisitCode(detailsWithDni(2), " "), TODAY).getNumber());
    }

    private static PatientDetails withVisitCode(PatientDetails d, String visitCode) {
        return new PatientDetails(d.name(), d.surname(), d.dni(), d.birthDate(), d.address(), d.phoneNumber(),
                d.sip(), visitCode, d.belongings(), d.abuseSubstances(), d.extraInfo());
    }

    @Test
    void rejectsThirtyThirdPatient() {
        for (int i = 1; i <= 32; i++) {
            useCase.execute(detailsWithDni(i), TODAY);
        }

        assertThrows(CenterFullException.class, () -> useCase.execute(detailsWithDni(33), TODAY));
    }
}
