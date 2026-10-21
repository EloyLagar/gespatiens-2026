package com.viller.gespatiens.diary.application;

import com.viller.gespatiens.diary.application.DiaryDayView.ActivityView;
import com.viller.gespatiens.diary.application.DiaryDayView.AttendanceRow;
import com.viller.gespatiens.diary.application.DiaryDayView.OutingView;
import com.viller.gespatiens.diary.application.DiaryDayView.ShiftView;
import com.viller.gespatiens.diary.domain.Activity;
import com.viller.gespatiens.diary.domain.ActivityRepository;
import com.viller.gespatiens.diary.domain.ActivityType;
import com.viller.gespatiens.diary.domain.AttendanceStatus;
import com.viller.gespatiens.diary.domain.DayPart;
import com.viller.gespatiens.diary.domain.DiaryConflictException;
import com.viller.gespatiens.diary.domain.DiaryEntryNotFoundException;
import com.viller.gespatiens.diary.domain.EditingPresence;
import com.viller.gespatiens.diary.domain.Evaluation;
import com.viller.gespatiens.diary.domain.Reduction;
import com.viller.gespatiens.diary.domain.ReductionNotFoundException;
import com.viller.gespatiens.diary.domain.ReductionRepository;
import com.viller.gespatiens.diary.domain.Resident;
import com.viller.gespatiens.diary.domain.ResidentDirectory;
import com.viller.gespatiens.diary.domain.ShiftLog;
import com.viller.gespatiens.diary.domain.ShiftLogRepository;
import com.viller.gespatiens.diary.domain.StaffDirectory;
import com.viller.gespatiens.diary.domain.StaffMember;
import com.viller.gespatiens.diary.domain.TherapeuticOuting;
import com.viller.gespatiens.diary.domain.TherapeuticOutingRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    // RF8: aquí nadie más está editando; los bloqueos se prueban en el módulo editing
    private final List<String> lockChecks = new ArrayList<>();
    private final EditingPresence editing = (resource, employeeId) -> lockChecks.add(resource);
    private final InMemoryShifts shifts = new InMemoryShifts();
    private final InMemoryReductions reductions = new InMemoryReductions();
    private final InMemoryActivities activities = new InMemoryActivities();
    private final InMemoryOutings outings = new InMemoryOutings();

    private final GetDiaryDayUseCase getDay =
            new GetDiaryDayUseCase(shifts, reductions, activities, outings, staff, residents);
    private final RecordShiftUseCase recordShift = new RecordShiftUseCase(shifts, staff, editing, CLOCK);
    private final RecordReductionUseCase recordReduction = new RecordReductionUseCase(reductions, residents, CLOCK);
    private final DeleteReductionUseCase deleteReduction = new DeleteReductionUseCase(reductions);
    private final ScheduleActivityUseCase scheduleActivity =
            new ScheduleActivityUseCase(activities, reductions, residents);
    private final RescheduleActivityUseCase rescheduleActivity =
            new RescheduleActivityUseCase(activities, reductions, residents, editing);
    private final RecordAttendanceUseCase recordAttendance =
            new RecordAttendanceUseCase(activities, reductions, residents, editing, CLOCK);
    private final RecordEvaluationsUseCase recordEvaluations =
            new RecordEvaluationsUseCase(activities, reductions, residents, editing);
    private final RecordOutingUseCase recordOuting = new RecordOutingUseCase(outings, residents);
    private final UpdateOutingUseCase updateOuting = new UpdateOutingUseCase(outings, residents);
    private final DeleteOutingUseCase deleteOuting = new DeleteOutingUseCase(outings);
    private final MonthlyEvaluationsUseCase monthly = new MonthlyEvaluationsUseCase(activities, residents);
    private final RecordMarkUseCase recordMark = new RecordMarkUseCase(activities, reductions, editing);

    @Test
    void anEmptyDayStillShowsTheThreeShifts() {
        DiaryDayView day = getDay.execute(DAY);

        assertEquals(List.of(DayPart.MORNING, DayPart.AFTERNOON, DayPart.NIGHT),
                day.shifts().stream().map(ShiftView::part).toList());
        assertTrue(day.shifts().stream().allMatch(s -> s.updatedAt().isEmpty()));
        assertTrue(day.reductions().isEmpty());
        assertTrue(day.activities().isEmpty());
        assertTrue(day.outings().isEmpty());
    }

    @Test
    void recordingAShiftTwiceUpdatesTheSameRecord() {
        recordShift.execute(DAY, DayPart.MORNING, Set.of(pablo.id()), "Sin incidencias", pablo.id(), null);
        ShiftView updated = recordShift.execute(DAY, DayPart.MORNING, Set.of(pablo.id(), ana.id()),
                "Sin incidencias. Llamó la familia de Juan", ana.id(), null);

        assertEquals(1, shifts.logs.size());
        assertEquals(List.of("Ana", "Pablo"), updated.staff().stream().map(StaffMember::name).toList());
        assertEquals("Ana", updated.updatedBy().orElseThrow().name());
    }

    // RF8: quien guarda con la versión que tenía delante no pisa lo que otra persona guardó entretanto
    @Test
    void aShiftSavedOverSomeoneElsesChangeIsAConflict() {
        ShiftView first = recordShift.execute(DAY, DayPart.MORNING, Set.of(pablo.id()), "Sin incidencias",
                pablo.id(), 0L);
        assertEquals(1, first.version());
        // Ana vio el turno vacío (versión 0) y guarda después de Pablo
        assertThrows(DiaryConflictException.class, () ->
                recordShift.execute(DAY, DayPart.MORNING, Set.of(ana.id()), "Otra cosa", ana.id(), 0L));

        ShiftView second = recordShift.execute(DAY, DayPart.MORNING, Set.of(ana.id()), "Otra cosa", ana.id(), 1L);
        assertEquals(2, second.version());
        assertEquals(List.of("shift:" + DAY + ":MORNING", "shift:" + DAY + ":MORNING", "shift:" + DAY + ":MORNING"),
                lockChecks);
        assertEquals(0, getDay.execute(DAY).shifts().get(1).version());
    }

    @Test
    void anActivityListSavedOverSomeoneElsesChangeIsAConflict() {
        ActivityView activity = scheduleActivity.execute(DAY, LocalTime.of(10, 0), ActivityType.SPORT, null);
        ActivityView marked = recordAttendance.execute(activity.id(),
                Map.of(juan.id(), AttendanceStatus.ATTENDED), pablo.id(), activity.version());

        assertThrows(DiaryConflictException.class, () -> recordAttendance.execute(activity.id(),
                Map.of(juan.id(), AttendanceStatus.EXCUSED_ABSENCE), ana.id(), activity.version()));
        assertThrows(DiaryConflictException.class, () -> recordEvaluations.execute(activity.id(),
                Map.of(juan.id(), new Evaluation(new BigDecimal("7"), null, null)), ana.id(), activity.version()));
        recordEvaluations.execute(activity.id(), Map.of(juan.id(), new Evaluation(new BigDecimal("7"), null, null)),
                ana.id(), marked.version());
        assertEquals("activity:" + activity.id(), lockChecks.getLast());
    }

    // RF6 en tabla mensual (memoria final): una fila por residente y una columna por día del mes
    @Test
    void theMonthlyGridShowsEachResidentsMarkPerDay() {
        UUID first = scheduleActivity.execute(DAY.minusDays(1), LocalTime.of(10, 0), ActivityType.LIFE_SKILLS, null).id();
        recordAttendance.execute(first, Map.of(juan.id(), AttendanceStatus.ATTENDED, lucia.id(),
                AttendanceStatus.UNEXCUSED_ABSENCE), pablo.id(), null);
        recordEvaluations.execute(first, Map.of(juan.id(), new Evaluation(new BigDecimal("8"), new BigDecimal("6"), null)),
                pablo.id(), null);
        // Dos actividades del tipo el mismo día: sale la media y la nota se cambia desde cada actividad
        UUID morning = scheduleActivity.execute(DAY, LocalTime.of(10, 0), ActivityType.LIFE_SKILLS, null).id();
        UUID evening = scheduleActivity.execute(DAY, LocalTime.of(17, 0), ActivityType.LIFE_SKILLS, null).id();
        recordAttendance.execute(morning, Map.of(juan.id(), AttendanceStatus.ATTENDED), pablo.id(), null);
        recordAttendance.execute(evening, Map.of(juan.id(), AttendanceStatus.ATTENDED), pablo.id(), null);
        recordEvaluations.execute(morning, Map.of(juan.id(), new Evaluation(new BigDecimal("6"), null, null)), pablo.id(), null);
        recordEvaluations.execute(evening, Map.of(juan.id(), new Evaluation(new BigDecimal("9"), null, null)), pablo.id(), null);
        scheduleActivity.execute(DAY, LocalTime.of(12, 0), ActivityType.SPORT, null);

        MonthlyEvaluationsView grid = monthly.execute(java.time.YearMonth.from(DAY), ActivityType.LIFE_SKILLS);

        assertEquals(DAY.lengthOfMonth(), grid.days().size());
        int yesterday = DAY.getDayOfMonth() - 2;
        int today = DAY.getDayOfMonth() - 1;
        assertEquals(1, grid.days().get(yesterday).activities().size());
        assertEquals(2, grid.days().get(today).activities().size());
        MonthlyEvaluationsView.ResidentRow juanRow = grid.rows().stream()
                .filter(r -> r.resident().id().equals(juan.id())).findFirst().orElseThrow();
        MonthlyEvaluationsView.ResidentRow luciaRow = grid.rows().stream()
                .filter(r -> r.resident().id().equals(lucia.id())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("8.0"), juanRow.cells().get(yesterday).mark().orElseThrow());
        assertEquals(first, juanRow.cells().get(yesterday).activityId().orElseThrow());
        assertEquals(new BigDecimal("7.5"), juanRow.cells().get(today).mark().orElseThrow());
        assertTrue(juanRow.cells().get(today).activityId().isEmpty());
        assertFalse(luciaRow.cells().get(yesterday).attended());
        assertTrue(luciaRow.cells().get(yesterday).activityId().isEmpty());
    }

    // Cambiar la nota desde la tabla conserva la satisfacción y la utilidad, y respeta RF8
    @Test
    void aMarkChangedFromTheGridKeepsTheRestAndChecksTheVersion() {
        ActivityView activity = scheduleActivity.execute(DAY, LocalTime.of(10, 0), ActivityType.LIFE_SKILLS, null);
        ActivityView marked = recordAttendance.execute(activity.id(), Map.of(juan.id(), AttendanceStatus.ATTENDED),
                pablo.id(), null);
        ActivityView evaluated = recordEvaluations.execute(activity.id(),
                Map.of(juan.id(), new Evaluation(new BigDecimal("5"), new BigDecimal("9"), null)), pablo.id(), null);

        long version = recordMark.execute(activity.id(), juan.id(), new BigDecimal("7.5"), ana.id(), evaluated.version());

        Evaluation now = activities.findById(activity.id()).orElseThrow().getEvaluations().get(juan.id());
        assertEquals(new BigDecimal("7.5"), now.mark());
        assertEquals(new BigDecimal("9.0"), now.satisfaction());
        assertEquals(evaluated.version() + 1, version);
        assertThrows(DiaryConflictException.class, () ->
                recordMark.execute(activity.id(), juan.id(), new BigDecimal("4"), ana.id(), marked.version()));
        assertThrows(IllegalArgumentException.class, () ->
                recordMark.execute(activity.id(), lucia.id(), new BigDecimal("4"), ana.id(), version));
        recordMark.execute(activity.id(), juan.id(), null, ana.id(), version);
        assertEquals(new BigDecimal("9.0"), activities.findById(activity.id()).orElseThrow()
                .getEvaluations().get(juan.id()).satisfaction());
        assertTrue(activities.findById(activity.id()).orElseThrow().getEvaluations().get(juan.id()).mark() == null);
    }

    @Test
    void unknownEmployeeInAShiftIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                recordShift.execute(DAY, DayPart.NIGHT, Set.of(UUID.randomUUID()), null, pablo.id(), null));
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

    @Test
    void theAttendanceListHasTheResidentsInTheCentreByPlaceNumber() {
        ActivityView activity = scheduleActivity.execute(DAY, LocalTime.of(10, 0), ActivityType.LIFE_SKILLS, null);

        assertEquals(List.of("Juan", "Lucía"),
                activity.attendance().stream().map(row -> row.resident().name()).toList());
        assertTrue(activity.attendance().stream().allMatch(row -> row.status().isEmpty() && !row.reduced()));
    }

    // "Rebajado" no se elige: sale de la rebaja, y lo que se anote para esa persona se descarta
    @Test
    void aReducedResidentShowsAsReducedAndIsNotMarked() {
        recordReduction.execute(DAY, lucia.id(), null, true, false, laura.id());
        UUID walk = scheduleActivity.execute(DAY, LocalTime.of(17, 0), ActivityType.WALK, null).id();

        ActivityView marked = recordAttendance.execute(walk, Map.of(
                juan.id(), AttendanceStatus.ATTENDED, lucia.id(), AttendanceStatus.UNEXCUSED_ABSENCE), pablo.id(), null);

        AttendanceRow juanRow = marked.attendance().get(0);
        AttendanceRow luciaRow = marked.attendance().get(1);
        assertEquals(AttendanceStatus.ATTENDED, juanRow.status().orElseThrow());
        assertTrue(luciaRow.reduced());
        assertTrue(luciaRow.status().isEmpty());
        assertFalse(activities.findById(walk).orElseThrow().getAttendance().containsKey(lucia.id()));
    }

    @Test
    void aSportReductionOnlyExcusesPhysicalActivitiesAndThePartialOneIsShown() {
        recordReduction.execute(DAY, juan.id(), "No puede cargar peso", false, true, laura.id());
        scheduleActivity.execute(DAY, LocalTime.of(10, 0), ActivityType.SPORT, null);
        scheduleActivity.execute(DAY, LocalTime.of(12, 0), ActivityType.VIDEO_FORUM, null);

        List<ActivityView> day = getDay.execute(DAY).activities();
        AttendanceRow inSport = day.get(0).attendance().get(0);
        AttendanceRow inForum = day.get(1).attendance().get(0);
        assertTrue(inSport.reduced());
        assertFalse(inForum.reduced());
        assertEquals("No puede cargar peso", inForum.partialReduction().orElseThrow());
    }

    @Test
    void evaluationsAreShownWithTheirAverages() {
        UUID id = scheduleActivity.execute(DAY, LocalTime.of(10, 0), ActivityType.VIDEO_FORUM, null).id();
        recordAttendance.execute(id, Map.of(juan.id(), AttendanceStatus.ATTENDED, lucia.id(), AttendanceStatus.ATTENDED), pablo.id(), null);

        ActivityView evaluated = recordEvaluations.execute(id, Map.of(
                juan.id(), new Evaluation(new BigDecimal("8"), new BigDecimal("9"), null),
                lucia.id(), new Evaluation(new BigDecimal("6.5"), new BigDecimal("6"), null)), pablo.id(), null);

        assertEquals(new BigDecimal("8.0"), evaluated.attendance().get(0).evaluation().orElseThrow().mark());
        assertEquals(new BigDecimal("7.3"), evaluated.averages().mark().orElseThrow());
        assertEquals(new BigDecimal("7.5"), evaluated.averages().satisfaction().orElseThrow());
        assertTrue(evaluated.averages().utility().isEmpty());
        assertEquals(evaluated, getDay.execute(DAY).activities().get(0));
    }

    @Test
    void absentOrReducedResidentsCannotBeEvaluated() {
        recordReduction.execute(DAY, lucia.id(), null, true, false, laura.id());
        UUID id = scheduleActivity.execute(DAY, LocalTime.of(10, 0), ActivityType.MAINTENANCE, null).id();
        recordAttendance.execute(id, Map.of(juan.id(), AttendanceStatus.UNEXCUSED_ABSENCE), pablo.id(), null);
        Evaluation five = new Evaluation(new BigDecimal("5"), null, null);

        assertThrows(IllegalArgumentException.class, () -> recordEvaluations.execute(id, Map.of(juan.id(), five), pablo.id(), null));
        assertThrows(IllegalArgumentException.class, () -> recordEvaluations.execute(id, Map.of(lucia.id(), five), pablo.id(), null));
        assertThrows(DiaryEntryNotFoundException.class, () -> recordEvaluations.execute(UUID.randomUUID(), Map.of(), pablo.id(), null));
    }

    @Test
    void attendanceOfAFutureActivityIsRejected() {
        UUID tomorrow = scheduleActivity.execute(DAY.plusDays(1), LocalTime.of(10, 0), ActivityType.SPORT, null).id();

        assertThrows(IllegalArgumentException.class, () ->
                recordAttendance.execute(tomorrow, Map.of(juan.id(), AttendanceStatus.ATTENDED), pablo.id(), null));
    }

    @Test
    void reschedulingKeepsTheListAlreadyTaken() {
        UUID id = scheduleActivity.execute(DAY, LocalTime.of(10, 0), ActivityType.SPORT, null).id();
        recordAttendance.execute(id, Map.of(juan.id(), AttendanceStatus.EXCUSED_ABSENCE), pablo.id(), null);

        ActivityView moved = rescheduleActivity.execute(id, LocalTime.of(11, 30), ActivityType.OTHER, "Piscina", pablo.id(), null);

        assertEquals("Piscina", moved.title().orElseThrow());
        assertEquals(AttendanceStatus.EXCUSED_ABSENCE, moved.attendance().get(0).status().orElseThrow());
    }

    @Test
    void unknownActivityOrOutingIsNotFound() {
        assertThrows(DiaryEntryNotFoundException.class, () ->
                rescheduleActivity.execute(UUID.randomUUID(), LocalTime.NOON, ActivityType.WALK, null, pablo.id(), null));
        assertThrows(DiaryEntryNotFoundException.class, () -> deleteOuting.execute(UUID.randomUUID()));
    }

    @Test
    void anOutingIsRecordedAndThenItsReturn() {
        UUID id = recordOuting.execute(DAY, lucia.id(), LocalTime.of(11, 0), "Con su madre").id();
        OutingView back = updateOuting.execute(id, LocalTime.of(11, 0), LocalTime.of(19, 15), "Con su madre");

        assertEquals(LocalTime.of(19, 15), back.returnedAt().orElseThrow());
        assertEquals(List.of("Lucía"), getDay.execute(DAY).outings().stream().map(o -> o.resident().name()).toList());
    }

    @Test
    void onlyResidentsInTheCentreCanGoOnAnOuting() {
        assertThrows(IllegalArgumentException.class, () ->
                recordOuting.execute(DAY, former.id(), LocalTime.of(11, 0), null));
    }

    private static class InMemoryShifts implements ShiftLogRepository {
        final List<ShiftLog> logs = new ArrayList<>();

        @Override
        public ShiftLog save(ShiftLog shiftLog) {
            // Como JPA: cada guardado sube la revisión (la primera es 0)
            ShiftLog saved = ShiftLog.reconstitute(shiftLog.getId(), shiftLog.getDate(), shiftLog.getPart(),
                    shiftLog.getStaffIds(), shiftLog.getNotes().orElse(null), shiftLog.getUpdatedBy().orElse(null),
                    shiftLog.getUpdatedAt(), shiftLog.getRevision().map(r -> r + 1).orElse(0L));
            logs.removeIf(l -> l.getId().equals(saved.getId()));
            logs.add(saved);
            return saved;
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

    private static class InMemoryActivities implements ActivityRepository {
        final Map<UUID, Activity> all = new HashMap<>();

        @Override
        public Activity save(Activity activity) {
            // Como JPA: cada guardado sube la revisión (la primera es 0)
            Activity saved = Activity.reconstitute(activity.getId(), activity.getDate(), activity.getStartTime(),
                    activity.getType(), activity.getTitle().orElse(null), activity.getAttendance(),
                    activity.getEvaluations(), activity.getRevision().map(r -> r + 1).orElse(0L));
            all.put(saved.getId(), saved);
            return saved;
        }

        @Override
        public Optional<Activity> findById(UUID id) {
            return Optional.ofNullable(all.get(id));
        }

        @Override
        public List<Activity> findBetween(LocalDate from, LocalDate to) {
            return all.values().stream().filter(a -> !a.getDate().isBefore(from) && !a.getDate().isAfter(to))
                    .sorted(Comparator.comparing(Activity::getDate).thenComparing(Activity::getStartTime)).toList();
        }

        @Override
        public List<Activity> findByDate(LocalDate date) {
            return all.values().stream().filter(a -> a.getDate().equals(date))
                    .sorted(Comparator.comparing(Activity::getStartTime)).toList();
        }

        @Override
        public void delete(Activity activity) {
            all.remove(activity.getId());
        }
    }

    private static class InMemoryOutings implements TherapeuticOutingRepository {
        final Map<UUID, TherapeuticOuting> all = new HashMap<>();

        @Override
        public TherapeuticOuting save(TherapeuticOuting outing) {
            all.put(outing.getId(), outing);
            return outing;
        }

        @Override
        public Optional<TherapeuticOuting> findById(UUID id) {
            return Optional.ofNullable(all.get(id));
        }

        @Override
        public List<TherapeuticOuting> findByDate(LocalDate date) {
            return all.values().stream().filter(o -> o.getDate().equals(date))
                    .sorted(Comparator.comparing(TherapeuticOuting::getLeftAt)).toList();
        }

        @Override
        public void delete(TherapeuticOuting outing) {
            all.remove(outing.getId());
        }
    }
}
