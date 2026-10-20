package com.viller.gespatiens.interventions.application;

import com.viller.gespatiens.interventions.domain.Intervention;
import com.viller.gespatiens.interventions.domain.InterventionNotFoundException;
import com.viller.gespatiens.interventions.domain.InterventionNotManageableException;
import com.viller.gespatiens.interventions.domain.InterventionRepository;
import com.viller.gespatiens.interventions.domain.InterventionStats;
import com.viller.gespatiens.interventions.domain.NotATutorException;
import com.viller.gespatiens.interventions.domain.Resident;
import com.viller.gespatiens.interventions.domain.ResidentDirectory;
import com.viller.gespatiens.interventions.domain.ResidentNotFoundException;
import com.viller.gespatiens.interventions.domain.ResidentTutors;
import com.viller.gespatiens.interventions.domain.ResidentTutorsRepository;
import com.viller.gespatiens.interventions.domain.StaffDirectory;
import com.viller.gespatiens.interventions.domain.StaffMember;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Test unitario puro: sin Spring ni BD
class InterventionUseCasesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 21);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"), ZoneOffset.UTC);

    private final StaffMember pablo = new StaffMember(UUID.randomUUID(), "Pablo", "SOCIAL_EDUCATOR", false);
    private final StaffMember ana = new StaffMember(UUID.randomUUID(), "Ana", "SOCIAL_EDUCATOR", false);
    private final StaffMember marcos = new StaffMember(UUID.randomUUID(), "Marcos", "PSYCHOLOGIST", false);
    private final StaffMember admin = new StaffMember(UUID.randomUUID(), "Rosa", "ADMINISTRATOR", true);
    private final Resident lucia = new Resident(UUID.randomUUID(), 5, "Lucía", "Martínez", true);
    private final Resident juan = new Resident(UUID.randomUUID(), 1, "Juan", "García", true);
    private final Resident former = new Resident(UUID.randomUUID(), null, "Marta", "Ruiz", false);

    private final StaffDirectory staff = new StaffDirectory() {
        private final Map<UUID, StaffMember> all = Map.of(pablo.id(), pablo, ana.id(), ana, marcos.id(), marcos,
                admin.id(), admin);

        @Override
        public Optional<StaffMember> find(UUID employeeId) {
            return Optional.ofNullable(all.get(employeeId));
        }

        @Override
        public Map<UUID, StaffMember> findAll(Collection<UUID> employeeIds) {
            return employeeIds.stream().filter(all::containsKey).distinct()
                    .collect(Collectors.toMap(id -> id, all::get));
        }
    };
    private final ResidentDirectory residents = new ResidentDirectory() {
        private final List<Resident> all = List.of(lucia, juan, former);

        @Override
        public Optional<Resident> find(UUID patientId) {
            return all.stream().filter(r -> r.id().equals(patientId)).findFirst();
        }

        @Override
        public List<Resident> findActive() {
            return all.stream().filter(Resident::active).sorted(Comparator.comparing(Resident::number)).toList();
        }
    };

    private final InMemoryTutors tutors = new InMemoryTutors();
    private final InMemoryInterventions interventions = new InMemoryInterventions();

    private final ListFollowUpsUseCase list = new ListFollowUpsUseCase(residents, tutors, interventions, staff);
    private final GetFollowUpUseCase get = new GetFollowUpUseCase(residents, tutors, interventions, staff);
    private final AssignTutorsUseCase assign = new AssignTutorsUseCase(residents, tutors, staff);
    private final RecordInterventionUseCase record =
            new RecordInterventionUseCase(residents, tutors, interventions, staff, CLOCK);
    private final EditInterventionUseCase edit = new EditInterventionUseCase(interventions, staff, CLOCK);
    private final DeleteInterventionUseCase delete = new DeleteInterventionUseCase(interventions, staff);

    @Test
    void theListHasTheResidentsInTheCentreWithTheirTutorsAndLastIntervention() {
        assign.execute(lucia.id(), Set.of(pablo.id(), ana.id()));
        record.execute(lucia.id(), pablo.id(), TODAY.minusDays(2), "Tutoría");
        record.execute(lucia.id(), ana.id(), TODAY, "Sesión de psicoterapia");

        List<FollowUpSummary> summaries = list.execute(pablo.id());

        assertEquals(List.of("Juan", "Lucía"), summaries.stream().map(s -> s.resident().name()).toList());
        FollowUpSummary luciaSummary = summaries.get(1);
        assertEquals(List.of("Ana", "Pablo"), luciaSummary.tutors().stream().map(StaffMember::name).toList());
        assertEquals(2, luciaSummary.interventionCount());
        assertEquals(TODAY, luciaSummary.lastIntervention().orElseThrow());
        assertTrue(luciaSummary.viewerIsTutor());
        assertEquals(0, summaries.get(0).interventionCount());
        assertFalse(summaries.get(0).viewerIsTutor());
    }

    // La memoria: las intervenciones "únicamente podrán generar los tutores de cada usuario"
    @Test
    void onlyTutorsRecordInterventions() {
        assign.execute(lucia.id(), Set.of(pablo.id()));

        assertThrows(NotATutorException.class, () -> record.execute(lucia.id(), ana.id(), TODAY, "Tutoría"));
        assertThrows(NotATutorException.class, () -> record.execute(lucia.id(), admin.id(), TODAY, "Tutoría"));
        InterventionView view = record.execute(lucia.id(), pablo.id(), TODAY, "Tutoría");
        assertTrue(view.mine());
        assertTrue(view.manageable());
        assertEquals("Pablo", view.author().orElseThrow().name());
    }

    @Test
    void theFollowUpListsInterventionsNewestFirstAndSaysWhatTheViewerCanDo() {
        assign.execute(lucia.id(), Set.of(pablo.id(), ana.id()));
        record.execute(lucia.id(), pablo.id(), TODAY.minusDays(5), "Primera tutoría");
        record.execute(lucia.id(), ana.id(), TODAY, "Sesión de psicoterapia");

        FollowUpView asPablo = get.execute(lucia.id(), pablo.id());
        FollowUpView asAdmin = get.execute(lucia.id(), admin.id());

        assertTrue(asPablo.viewerIsTutor());
        assertEquals(List.of("Sesión de psicoterapia", "Primera tutoría"),
                asPablo.interventions().stream().map(i -> i.intervention().getText()).toList());
        assertEquals(List.of(false, true), asPablo.interventions().stream().map(InterventionView::manageable).toList());
        assertFalse(asAdmin.viewerIsTutor());
        assertTrue(asAdmin.interventions().stream().allMatch(InterventionView::manageable));
    }

    @Test
    void tutorsAreOnlyAssignedToResidentsInTheCentreAndMustExist() {
        assertThrows(IllegalArgumentException.class, () -> assign.execute(former.id(), Set.of(pablo.id())));
        assertThrows(IllegalArgumentException.class, () -> assign.execute(lucia.id(), Set.of(UUID.randomUUID())));
        assertThrows(ResidentNotFoundException.class, () -> assign.execute(UUID.randomUUID(), Set.of()));
        assertEquals(List.of(), assign.execute(lucia.id(), Set.of()));
    }

    // Solo los educadores sociales son tutores: ni la administración ni el resto de especialidades
    @Test
    void onlySocialEducatorsCanBeTutors() {
        assertThrows(IllegalArgumentException.class, () -> assign.execute(lucia.id(), Set.of(pablo.id(), admin.id())));
        assertThrows(IllegalArgumentException.class, () -> assign.execute(lucia.id(), Set.of(marcos.id())));
        assertEquals(List.of("Pablo"), assign.execute(lucia.id(), Set.of(pablo.id())).stream()
                .map(StaffMember::name).toList());
    }

    // El historial de un antiguo residente se consulta, pero ya no se le añaden intervenciones
    @Test
    void aFormerResidentKeepsTheHistoryButGetsNoNewInterventions() {
        tutors.save(ResidentTutors.reconstitute(former.id(), Set.of(pablo.id())));
        interventions.save(Intervention.record(former.id(), pablo.id(), TODAY.minusDays(30), "Tutoría de salida",
                CLOCK.instant(), TODAY));

        assertThrows(IllegalArgumentException.class, () -> record.execute(former.id(), pablo.id(), TODAY, "Tutoría"));
        assertEquals(1, get.execute(former.id(), pablo.id()).interventions().size());
    }

    @Test
    void onlyTheAuthorOrAdministrationEditOrDelete() {
        assign.execute(lucia.id(), Set.of(pablo.id(), ana.id()));
        UUID id = record.execute(lucia.id(), pablo.id(), TODAY, "Tutoría").intervention().getId();

        assertThrows(InterventionNotManageableException.class, () -> edit.execute(id, ana.id(), TODAY, "Otra cosa"));
        assertThrows(InterventionNotManageableException.class, () -> delete.execute(id, ana.id()));
        InterventionView edited = edit.execute(id, admin.id(), TODAY.minusDays(1), "Tutoría individual");
        assertEquals("Tutoría individual", edited.intervention().getText());
        assertFalse(edited.mine());
        delete.execute(id, pablo.id());
        assertThrows(InterventionNotFoundException.class, () -> delete.execute(id, pablo.id()));
    }

    private static class InMemoryTutors implements ResidentTutorsRepository {
        private final Map<UUID, ResidentTutors> byPatient = new HashMap<>();

        @Override
        public ResidentTutors save(ResidentTutors tutors) {
            byPatient.put(tutors.getPatientId(), ResidentTutors.reconstitute(tutors.getPatientId(), tutors.getTutorIds()));
            return tutors;
        }

        @Override
        public ResidentTutors findByPatient(UUID patientId) {
            ResidentTutors stored = byPatient.get(patientId);
            return stored == null
                    ? ResidentTutors.none(patientId)
                    : ResidentTutors.reconstitute(patientId, stored.getTutorIds());
        }

        @Override
        public Map<UUID, ResidentTutors> findAll() {
            return Map.copyOf(byPatient);
        }
    }

    private static class InMemoryInterventions implements InterventionRepository {
        private final Map<UUID, Intervention> byId = new HashMap<>();

        @Override
        public Intervention save(Intervention intervention) {
            byId.put(intervention.getId(), intervention);
            return intervention;
        }

        @Override
        public Optional<Intervention> findById(UUID id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public List<Intervention> findByPatient(UUID patientId) {
            return byId.values().stream()
                    .filter(i -> i.getPatientId().equals(patientId))
                    .sorted(Comparator.comparing(Intervention::getDate).thenComparing(Intervention::getCreatedAt).reversed())
                    .toList();
        }

        @Override
        public Map<UUID, InterventionStats> statsByPatient() {
            return byId.values().stream().collect(Collectors.groupingBy(Intervention::getPatientId,
                    Collectors.collectingAndThen(Collectors.toList(), list -> new InterventionStats(list.size(),
                            list.stream().map(Intervention::getDate).max(Comparator.naturalOrder()).orElseThrow()))));
        }

        @Override
        public void delete(Intervention intervention) {
            byId.remove(intervention.getId());
        }
    }
}
