package com.viller.gespatiens.patients.application;

import com.viller.gespatiens.patients.domain.Dni;
import com.viller.gespatiens.patients.domain.Patient;
import com.viller.gespatiens.patients.domain.PatientDetails;
import com.viller.gespatiens.patients.domain.PatientNotFoundException;
import com.viller.gespatiens.patients.domain.Referral;
import com.viller.gespatiens.patients.domain.ReferralRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Test unitario puro: sin Spring ni BD
class ReferralUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"), ZoneOffset.UTC);

    private final InMemoryPatientRepository patients = new InMemoryPatientRepository();
    private final Map<UUID, Referral> stored = new HashMap<>();
    private final ReferralRepository referrals = new ReferralRepository() {
        @Override
        public Referral save(Referral referral) {
            stored.put(referral.getPatientId(), referral);
            return referral;
        }

        @Override
        public Optional<Referral> findByPatient(UUID patientId) {
            return Optional.ofNullable(stored.get(patientId));
        }
    };
    private final ReferralUseCase useCase = new ReferralUseCase(patients, referrals, CLOCK);

    private UUID patient() {
        PatientDetails details = new PatientDetails("Juan", "García", new Dni("12345678Z"), LocalDate.of(1990, 1, 1),
                "C/ Mayor 1", "600111222", null, null, null, null, null);
        return patients.save(Patient.admit(details, 1, LocalDate.of(2026, 4, 1))).getId();
    }

    @Test
    void aResidentWithoutReferralShowsItEmpty() {
        Referral referral = useCase.get(patient());

        assertNull(referral.getReferringInstitution());
        assertFalse(referral.isCourtOrdered());
        assertThrows(PatientNotFoundException.class, () -> useCase.get(UUID.randomUUID()));
    }

    // Memoria final, 5.2: muchos llegan obligados judicialmente, derivados por una institución
    @Test
    void theReferralKeepsTheInstitutionAndTheCourtOrder() {
        UUID juan = patient();

        useCase.update(juan, " UCA Torrent ", "Marta Gil", "961112233", true, "Juzgado nº 3 de Valencia, 123/2026",
                LocalDate.of(2027, 4, 1), "Seguimiento mensual con la UCA");

        Referral referral = useCase.get(juan);
        assertEquals("UCA Torrent", referral.getReferringInstitution());
        assertTrue(referral.isCourtOrdered());
        assertEquals(LocalDate.of(2027, 4, 1), referral.getMeasureEndDate());
        assertEquals("Seguimiento mensual con la UCA", referral.getNotes());
    }

    // Sin medida judicial no quedan un juzgado ni una fecha escondidos
    @Test
    void withoutCourtOrderTheCourtDataIsDropped() {
        UUID juan = patient();

        Referral referral = useCase.update(juan, "Servicios sociales", null, null, false, "Juzgado nº 3",
                LocalDate.of(2027, 4, 1), null);

        assertNull(referral.getCourt());
        assertNull(referral.getMeasureEndDate());
    }

    @Test
    void longTextsAreRejected() {
        UUID juan = patient();

        assertThrows(IllegalArgumentException.class, () -> useCase.update(juan, "x".repeat(151), null, null, false,
                null, null, null));
        assertThrows(IllegalArgumentException.class, () -> useCase.update(juan, null, null, null, false, null, null,
                "x".repeat(Referral.MAX_NOTES_LENGTH + 1)));
    }
}
