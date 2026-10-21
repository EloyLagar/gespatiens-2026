package com.viller.gespatiens.reports.application;

import com.viller.gespatiens.reports.domain.AreaNotWritableException;
import com.viller.gespatiens.reports.domain.EditingPresence;
import com.viller.gespatiens.reports.domain.Report;
import com.viller.gespatiens.reports.domain.ReportArea;
import com.viller.gespatiens.reports.domain.ReportNotFoundException;
import com.viller.gespatiens.reports.domain.ReportRepository;
import com.viller.gespatiens.reports.domain.ReportSection;
import com.viller.gespatiens.reports.domain.ReportType;
import com.viller.gespatiens.reports.domain.Resident;
import com.viller.gespatiens.reports.domain.ResidentDirectory;
import com.viller.gespatiens.reports.domain.SignatureDirectory;
import com.viller.gespatiens.reports.domain.StaffDirectory;
import com.viller.gespatiens.reports.domain.StaffMember;
import com.viller.gespatiens.reports.domain.TutorDirectory;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Test unitario puro: sin Spring ni BD
class ReportUseCasesTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate ENTRY = LocalDate.of(2026, 4, 1);

    private final StaffMember marcos = new StaffMember(UUID.randomUUID(), "Marcos", "PSYCHOLOGIST", false);
    private final StaffMember pablo = new StaffMember(UUID.randomUUID(), "Pablo", "SOCIAL_EDUCATOR", false);
    private final StaffMember rosa = new StaffMember(UUID.randomUUID(), "Rosa", "ADMINISTRATOR", true);

    private final Map<UUID, Resident> residentsById = new HashMap<>();
    private Resident lucia = resident("Lucía", "Martínez", 5, ENTRY, null);
    private final Resident marta = resident("Marta", "Ruiz", null, ENTRY, LocalDate.of(2026, 10, 1));

    private Resident resident(String name, String surname, Integer number, LocalDate entry, LocalDate exit) {
        Resident r = new Resident(UUID.randomUUID(), number, name, surname, "12345678Z", LocalDate.of(1990, 5, 3),
                "SIP-1", entry, exit, exit == null);
        residentsById.put(r.id(), r);
        return r;
    }

    // Lucía sale y vuelve a entrar: misma ficha, estancia nueva
    private void readmitLucia(LocalDate exit, LocalDate entry) {
        Resident discharged = new Resident(lucia.id(), null, lucia.name(), lucia.surname(), lucia.dni(),
                lucia.birthDate(), lucia.sip(), lucia.entryDate(), exit, false);
        residentsById.put(lucia.id(), discharged);
        stay.stayChanged(lucia.id());
        lucia = new Resident(lucia.id(), 7, lucia.name(), lucia.surname(), lucia.dni(), lucia.birthDate(),
                lucia.sip(), entry, null, true);
        residentsById.put(lucia.id(), lucia);
        stay.stayChanged(lucia.id());
    }

    private final StaffDirectory staff = new StaffDirectory() {
        private final Map<UUID, StaffMember> all = Map.of(marcos.id(), marcos, pablo.id(), pablo, rosa.id(), rosa);

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
        @Override
        public Optional<Resident> find(UUID patientId) {
            return Optional.ofNullable(residentsById.get(patientId));
        }

        @Override
        public Map<UUID, Resident> findAll(Collection<UUID> patientIds) {
            return patientIds.stream().filter(residentsById::containsKey).distinct()
                    .collect(Collectors.toMap(id -> id, residentsById::get));
        }

        @Override
        public List<Resident> findActive() {
            return residentsById.values().stream().filter(Resident::active).toList();
        }
    };
    // Pablo es tutor de Lucía
    private final TutorDirectory tutors = (patientId, employeeId) ->
            patientId.equals(lucia.id()) && employeeId.equals(pablo.id());
    private final List<String> lockChecks = new ArrayList<>();
    private final EditingPresence editing = (resource, employeeId) -> lockChecks.add(resource);
    private final SignatureDirectory signatures = employeeId ->
            employeeId.equals(pablo.id()) ? Optional.of(new byte[]{9}) : Optional.empty();
    private final InMemoryReports reports = new InMemoryReports();
    private final List<List<PrintableReport>> rendered = new ArrayList<>();
    private final ReportPdfRenderer renderer = printable -> {
        rendered.add(printable);
        return new byte[]{1, 2, 3};
    };

    private final StayReportsUseCase stay = new StayReportsUseCase(reports, residents, CLOCK);
    private final GetReportUseCase get = new GetReportUseCase(reports, residents, staff, tutors);
    private final ListReportsUseCase list = new ListReportsUseCase(reports, residents, stay);
    private final ListResidentReportsUseCase ofResident = new ListResidentReportsUseCase(stay, residents);
    private final WriteSectionUseCase write = new WriteSectionUseCase(reports, residents, staff, tutors, editing, CLOCK);
    private final EditReportHeaderUseCase header =
            new EditReportHeaderUseCase(reports, residents, staff, tutors, editing);
    private final CloseReportUseCase close = new CloseReportUseCase(reports, residents, staff, tutors, CLOCK);
    private final ExportReportsPdfUseCase export =
            new ExportReportsPdfUseCase(reports, residents, staff, signatures, renderer);

    private UUID reportOf(Resident resident, ReportType type) {
        return ofResident.execute(resident.id()).reports().stream()
                .filter(r -> r.getType() == type && r.getStayStart().equals(resident.entryDate()))
                .findFirst().orElseThrow().getId();
    }

    // "Cada vez que entre alguien se debería crear un informe vacío"
    @Test
    void eachStayGetsOneEmptyReportOfEachType() {
        stay.stayChanged(lucia.id());
        stay.stayChanged(lucia.id());

        List<Report> own = ofResident.execute(lucia.id()).reports();

        assertEquals(2, own.size());
        assertEquals(Set.of(ReportType.MID_STAY, ReportType.FINAL),
                own.stream().map(Report::getType).collect(Collectors.toSet()));
        assertTrue(own.stream().allMatch(r -> r.getStayStart().equals(ENTRY) && r.writtenSections() == 0));
    }

    // Quien sale y vuelve conserva los informes de antes como historial y estrena otros
    @Test
    void aReadmissionKeepsTheOldReportsAndOpensNewOnes() {
        UUID oldFinal = reportOf(lucia, ReportType.FINAL);
        write.execute(oldFinal, ReportSection.ADAPTATION_HABITS, "Bien", 0, pablo.id());

        readmitLucia(LocalDate.of(2026, 6, 30), LocalDate.of(2026, 9, 1));

        List<Report> own = ofResident.execute(lucia.id()).reports();
        assertEquals(4, own.size());
        Report previous = get.execute(oldFinal, rosa.id()).report();
        assertEquals(LocalDate.of(2026, 6, 30), previous.getHeader().exitDate());
        assertEquals(1, previous.writtenSections());
        assertEquals(LocalDate.of(2026, 9, 1), get.execute(reportOf(lucia, ReportType.FINAL), rosa.id())
                .report().getStayStart());
    }

    // Los de quienes están en el centro aparecen en "Informes" aunque nadie los haya abierto
    @Test
    void theListIncludesTheReportsOfEveryoneInTheCentre() {
        List<ReportSummary> summaries = list.execute();

        assertEquals(2, summaries.size());
        assertTrue(summaries.stream().allMatch(s -> s.resident().id().equals(lucia.id())));
    }

    @Test
    void theStayEndReachesTheHeaderOfAFormerResident() {
        Report fin = get.execute(reportOf(marta, ReportType.FINAL), rosa.id()).report();

        assertEquals(LocalDate.of(2026, 10, 1), fin.getHeader().exitDate());
        assertEquals(ENTRY, fin.getHeader().entryDate());
    }

    @Test
    void writingASectionChecksTheLockAndResolvesItsAuthor() {
        UUID id = reportOf(lucia, ReportType.MID_STAY);

        ReportView view = write.execute(id, ReportSection.PSYCHO_DIAGNOSIS, "Dual", 0, marcos.id());

        assertEquals(List.of("report:" + id + ":section:PSYCHO_DIAGNOSIS"), lockChecks);
        assertEquals(marcos.id(), view.report().getSection(ReportSection.PSYCHO_DIAGNOSIS).orElseThrow().authorId());
        assertEquals("Marcos", view.staff().get(marcos.id()).name());
    }

    // El tutor redacta las conclusiones; otro psicólogo que no es su tutor, no
    @Test
    void tutorsWriteTheConclusions() {
        UUID id = reportOf(lucia, ReportType.MID_STAY);

        assertTrue(get.execute(id, pablo.id()).writableAreas().contains(ReportArea.DURATION));
        assertFalse(get.execute(id, marcos.id()).writableAreas().contains(ReportArea.DURATION));
        write.execute(id, ReportSection.PROGRAM_DURATION_FORECAST, "6 meses", 0, pablo.id());
        assertThrows(AreaNotWritableException.class,
                () -> write.execute(id, ReportSection.PROGRAM_DURATION_FORECAST, "8", 1, marcos.id()));
    }

    @Test
    void theHeaderIsCompletedByAnyEmployee() {
        UUID id = reportOf(lucia, ReportType.MID_STAY);

        ReportView view = header.execute(id, "S-1", "UCA Quart", "N-9", null, null, 0, marcos.id());

        assertEquals("UCA Quart", view.report().getHeader().referenceCenter());
        assertEquals(List.of("report:" + id + ":header"), lockChecks);
    }

    @Test
    void onlyAdministrationCloses() {
        UUID id = reportOf(lucia, ReportType.MID_STAY);

        close.close(id, rosa.id());

        assertFalse(get.execute(id, pablo.id()).report().isDraft());
        assertThrows(ReportNotFoundException.class, () -> get.execute(UUID.randomUUID(), rosa.id()));
    }

    // RF2: los de un tipo de todas las estancias, o los dos tipos juntos, en un solo PDF
    @Test
    void theResidentPdfGathersEveryStay() {
        UUID oldMid = reportOf(lucia, ReportType.MID_STAY);
        write.execute(oldMid, ReportSection.ADAPTATION_HABITS, "Bien", 0, pablo.id());
        readmitLucia(LocalDate.of(2026, 6, 30), LocalDate.of(2026, 9, 1));
        UUID newMid = reportOf(lucia, ReportType.MID_STAY);

        PdfFile mids = export.ofResident(lucia.id(), ReportType.MID_STAY);

        assertArrayEquals(new byte[]{1, 2, 3}, mids.content());
        assertEquals("informes-de-media-estancia-martinez-lucia.pdf", mids.fileName());
        List<PrintableReport> printed = rendered.getLast();
        assertEquals(List.of(oldMid, newMid), printed.stream().map(p -> p.report().getId()).toList());
        assertEquals("Pablo", printed.getFirst().authors().getFirst().member().name());
        assertArrayEquals(new byte[]{9}, printed.getFirst().authors().getFirst().signature().orElseThrow());

        PdfFile all = export.ofResident(lucia.id(), null);
        assertEquals("informes-martinez-lucia.pdf", all.fileName());
        assertEquals(List.of(ReportType.MID_STAY, ReportType.FINAL, ReportType.MID_STAY, ReportType.FINAL),
                rendered.getLast().stream().map(p -> p.report().getType()).toList());
    }

    private static final class InMemoryReports implements ReportRepository {

        private final Map<UUID, Report> all = new LinkedHashMap<>();

        @Override
        public Report save(Report report) {
            all.put(report.getId(), report);
            return report;
        }

        @Override
        public void addIfMissing(Report report) {
            boolean exists = all.values().stream().anyMatch(r -> r.getPatientId().equals(report.getPatientId())
                    && r.getType() == report.getType() && r.getStayStart().equals(report.getStayStart()));
            if (!exists) {
                save(report);
            }
        }

        @Override
        public Optional<Report> findById(UUID id) {
            return Optional.ofNullable(all.get(id));
        }

        @Override
        public List<Report> findByPatient(UUID patientId) {
            return all.values().stream().filter(r -> r.getPatientId().equals(patientId))
                    .sorted(Comparator.comparing(Report::getStayStart).thenComparing(Report::getType)).toList();
        }

        @Override
        public List<Report> findAll() {
            return List.copyOf(all.values());
        }
    }
}
