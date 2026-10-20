package com.viller.gespatiens.diary.application;

import com.viller.gespatiens.diary.application.DiaryDayView.ShiftView;
import com.viller.gespatiens.diary.domain.DayPart;
import com.viller.gespatiens.diary.domain.Reduction;
import com.viller.gespatiens.diary.domain.ReductionNotFoundException;
import com.viller.gespatiens.diary.domain.ReductionRepository;
import com.viller.gespatiens.diary.domain.Resident;
import com.viller.gespatiens.diary.domain.ResidentDirectory;
import com.viller.gespatiens.diary.domain.ShiftLog;
import com.viller.gespatiens.diary.domain.ShiftLogRepository;
import com.viller.gespatiens.diary.domain.StaffDirectory;
import com.viller.gespatiens.diary.domain.StaffMember;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Test unitario puro: sin Spring ni BD
class DiaryUseCasesTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 20);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-20T15:00:00Z"), ZoneOffset.UTC);

    private final StaffMember pablo = new StaffMember(UUID.randomUUID(), "Pablo", "SOCIAL_EDUCATOR");
    private final StaffMember ana = new StaffMember(UUID.randomUUID(), "Ana", "SOCIAL_EDUCATOR");
    private final StaffMember laura = new StaffMember(UUID.randomUUID(), "Laura", "MEDICAL_TEAM");
    private final Resident lucia = new Resident(UUID.randomUUID(), 5, "Lucía", "Martínez", true);
    private final Resident juan = new Resident(UUID.randomUUID(), 1, "Juan", "García", true);
    private final Resident former = new Resident(UUID.randomUUID(), null, "Marta", "Ruiz", false);

    private final StaffDirectory staff = id -> Optional.ofNullable(
            Map.of(pablo.id(), pablo, ana.id(), ana, laura.id(), laura).get(id));
    private final ResidentDirectory residents = id -> Optional.ofNullable(
            Map.of(lucia.id(), lucia, juan.id(), juan, former.id(), former).get(id));

    private final InMemoryShifts shifts = new InMemoryShifts();
    private final InMemoryReductions reductions = new InMemoryReductions();

    private final GetDiaryDayUseCase getDay = new GetDiaryDayUseCase(shifts, reductions, staff, residents);
    private final RecordShiftUseCase recordShift = new RecordShiftUseCase(shifts, staff, CLOCK);
    private final RecordReductionUseCase recordReduction = new RecordReductionUseCase(reductions, residents, CLOCK);
    private final DeleteReductionUseCase deleteReduction = new DeleteReductionUseCase(reductions);

    @Test
    void anEmptyDayStillShowsTheThreeShifts() {
        DiaryDayView day = getDay.execute(DAY);

        assertEquals(List.of(DayPart.MORNING, DayPart.AFTERNOON, DayPart.NIGHT),
                day.shifts().stream().map(ShiftView::part).toList());
        assertTrue(day.shifts().stream().allMatch(s -> s.updatedAt().isEmpty()));
        assertTrue(day.reductions().isEmpty());
    }

    @Test
    void recordingAShiftTwiceUpdatesTheSameRecord() {
        recordShift.execute(DAY, DayPart.MORNING, Set.of(pablo.id()), "Sin incidencias", pablo.id());
        ShiftView updated = recordShift.execute(DAY, DayPart.MORNING, Set.of(pablo.id(), ana.id()),
                "Sin incidencias. Llamó la familia de Juan", ana.id());

        assertEquals(1, shifts.logs.size());
        assertEquals(List.of("Ana", "Pablo"), updated.staff().stream().map(StaffMember::name).toList());
        assertEquals("Ana", updated.updatedBy().orElseThrow().name());
    }

    @Test
    void unknownEmployeeInAShiftIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                recordShift.execute(DAY, DayPart.NIGHT, Set.of(UUID.randomUUID()), null, pablo.id()));
    }

    @Test
    void reductionsAreListedByPlaceNumberAndReplacedNotDuplicated() {
        recordReduction.execute(DAY, lucia.id(), "No puede cargar peso", false, false, laura.id());
        recordReduction.execute(DAY, juan.id(), null, false, true, laura.id());
        recordReduction.execute(DAY, lucia.id(), null, true, false, laura.id());

        DiaryDayView day = getDay.execute(DAY);
        assertEquals(2, day.reductions().size());
        assertEquals("Juan", day.reductions().get(0).resident().name());
        assertTrue(day.reductions().get(1).total());
        assertTrue(day.reductions().get(1).partial().isEmpty());
    }

    @Test
    void onlyResidentsInTheCentreCanBeReduced() {
        assertThrows(IllegalArgumentException.class, () ->
                recordReduction.execute(DAY, former.id(), null, true, false, laura.id()));
        assertThrows(IllegalArgumentException.class, () ->
                recordReduction.execute(DAY, UUID.randomUUID(), null, true, false, laura.id()));
    }

    @Test
    void deletingAReductionThatDoesNotExistIsNotFound() {
        assertThrows(ReductionNotFoundException.class, () -> deleteReduction.execute(DAY, lucia.id()));
    }

    private static class InMemoryShifts implements ShiftLogRepository {
        final List<ShiftLog> logs = new ArrayList<>();

        @Override
        public ShiftLog save(ShiftLog shiftLog) {
            logs.removeIf(l -> l.getId().equals(shiftLog.getId()));
            logs.add(shiftLog);
            return shiftLog;
        }

        @Override
        public List<ShiftLog> findByDate(LocalDate date) {
            return logs.stream().filter(l -> l.getDate().equals(date)).toList();
        }

        @Override
        public Optional<ShiftLog> findByDateAndPart(LocalDate date, DayPart part) {
            return logs.stream().filter(l -> l.getDate().equals(date) && l.getPart() == part).findFirst();
        }
    }

    private static class InMemoryReductions implements ReductionRepository {
        final List<Reduction> all = new ArrayList<>();

        @Override
        public Reduction save(Reduction reduction) {
            all.removeIf(r -> r.getId().equals(reduction.getId()));
            all.add(reduction);
            return reduction;
        }

        @Override
        public List<Reduction> findByDate(LocalDate date) {
            return all.stream().filter(r -> r.getDate().equals(date)).toList();
        }

        @Override
        public Optional<Reduction> findByDateAndPatient(LocalDate date, UUID patientId) {
            return all.stream().filter(r -> r.getDate().equals(date) && r.getPatientId().equals(patientId)).findFirst();
        }

        @Override
        public void delete(Reduction reduction) {
            all.removeIf(r -> r.getId().equals(reduction.getId()));
        }
    }
}
