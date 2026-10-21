package com.viller.gespatiens.patients.application;

import com.viller.gespatiens.patients.domain.AuthorizedVisitor;
import com.viller.gespatiens.patients.domain.AuthorizedVisitorNotFoundException;
import com.viller.gespatiens.patients.domain.AuthorizedVisitorRepository;
import com.viller.gespatiens.patients.domain.Dni;
import com.viller.gespatiens.patients.domain.Patient;
import com.viller.gespatiens.patients.domain.PatientDetails;
import com.viller.gespatiens.patients.domain.PatientNotFoundException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Test unitario puro: sin Spring ni BD
class AuthorizedVisitorsUseCaseTest {

    private final InMemoryPatientRepository patients = new InMemoryPatientRepository();
    private final InMemoryVisitors visitors = new InMemoryVisitors();
    private final AuthorizedVisitorsUseCase useCase = new AuthorizedVisitorsUseCase(patients, visitors);

    private UUID patient(String dni) {
        PatientDetails details = new PatientDetails("Juan", "García", new Dni(dni), LocalDate.of(1990, 1, 1),
                "C/ Mayor 1", "600111222", null, null, null, null, null);
        return patients.save(Patient.admit(details, 1, LocalDate.of(2026, 4, 1))).getId();
    }

    // Memoria final: "las personas que están autorizadas a visitarles, la relación de estas con el usuario y su
    // número de teléfono"
    @Test
    void visitorsAreListedByNameWithRelationshipAndPhone() {
        UUID juan = patient("12345678Z");
        useCase.authorize(juan, "Rosa", "Madre", "600000001");
        useCase.authorize(juan, "Luis", "Hermano", "600000002");

        List<AuthorizedVisitor> list = useCase.list(juan);

        assertEquals(List.of("Luis", "Rosa"), list.stream().map(AuthorizedVisitor::getName).toList());
        assertEquals("Hermano", list.getFirst().getRelationship());
    }

    @Test
    void theyAreCorrectedAndRevoked() {
        UUID juan = patient("12345678Z");
        UUID rosa = useCase.authorize(juan, "Rosa", "Madre", "600000001").getId();

        assertEquals("611222333", useCase.update(juan, rosa, "Rosa", "Madre", " 611222333 ").getPhoneNumber());
        useCase.revoke(juan, rosa);

        assertTrue(useCase.list(juan).isEmpty());
    }

    // La URL lleva el residente: no se toca a la persona autorizada de otro
    @Test
    void someoneElsesVisitorIsNotFound() {
        UUID juan = patient("12345678Z");
        UUID other = patient("87654321X");
        UUID rosa = useCase.authorize(juan, "Rosa", "Madre", "600000001").getId();

        assertThrows(AuthorizedVisitorNotFoundException.class, () -> useCase.revoke(other, rosa));
        assertThrows(PatientNotFoundException.class, () -> useCase.list(UUID.randomUUID()));
    }

    @Test
    void nameRelationshipAndPhoneAreRequired() {
        UUID juan = patient("12345678Z");

        assertThrows(IllegalArgumentException.class, () -> useCase.authorize(juan, " ", "Madre", "600000001"));
        assertThrows(IllegalArgumentException.class, () -> useCase.authorize(juan, "Rosa", null, "600000001"));
        assertThrows(IllegalArgumentException.class, () -> useCase.authorize(juan, "Rosa", "Madre", ""));
    }

    @Test
    void theListHasALimit() {
        UUID juan = patient("12345678Z");
        for (int i = 0; i < AuthorizedVisitorsUseCase.MAX_PER_RESIDENT; i++) {
            useCase.authorize(juan, "Persona " + i, "Amistad", "6000000" + i);
        }

        assertThrows(IllegalArgumentException.class, () -> useCase.authorize(juan, "Una más", "Amistad", "699"));
    }

    private static final class InMemoryVisitors implements AuthorizedVisitorRepository {

        private final Map<UUID, AuthorizedVisitor> all = new LinkedHashMap<>();

        @Override
        public AuthorizedVisitor save(AuthorizedVisitor visitor) {
            all.put(visitor.getId(), visitor);
            return visitor;
        }

        @Override
        public Optional<AuthorizedVisitor> findById(UUID id) {
            return Optional.ofNullable(all.get(id));
        }

        @Override
        public List<AuthorizedVisitor> findByPatient(UUID patientId) {
            return all.values().stream().filter(v -> v.getPatientId().equals(patientId))
                    .sorted(Comparator.comparing(AuthorizedVisitor::getName)).toList();
        }

        @Override
        public void delete(AuthorizedVisitor visitor) {
            all.remove(visitor.getId());
        }
    }
}
