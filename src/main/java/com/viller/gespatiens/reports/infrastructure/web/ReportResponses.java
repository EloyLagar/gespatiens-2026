package com.viller.gespatiens.reports.infrastructure.web;

import com.viller.gespatiens.reports.application.ReportSummary;
import com.viller.gespatiens.reports.application.ReportView;
import com.viller.gespatiens.reports.application.ResidentReports;
import com.viller.gespatiens.reports.domain.Report;
import com.viller.gespatiens.reports.domain.ReportArea;
import com.viller.gespatiens.reports.domain.ReportHeader;
import com.viller.gespatiens.reports.domain.ReportSection;
import com.viller.gespatiens.reports.domain.ReportState;
import com.viller.gespatiens.reports.domain.ReportType;
import com.viller.gespatiens.reports.domain.Resident;
import com.viller.gespatiens.reports.domain.SectionEntry;
import com.viller.gespatiens.reports.domain.StaffMember;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

// DTO de salida de los informes
public final class ReportResponses {

    private ReportResponses() {
    }

    public record StaffResponse(UUID id, String name, String speciality) {
        static StaffResponse from(StaffMember member) {
            return member == null ? null : new StaffResponse(member.id(), member.name(), member.speciality());
        }
    }

    // number null: ya no está en el centro. Los datos de la ficha que van en la cabecera del informe
    public record ResidentResponse(UUID id, Integer number, String name, String surname, String dni,
                                   LocalDate birthDate, String sip, boolean active) {
        static ResidentResponse from(Resident r) {
            return new ResidentResponse(r.id(), r.number(), r.name(), r.surname(), r.dni(), r.birthDate(), r.sip(),
                    r.active());
        }
    }

    // Un informe en un listado. stayStart/stayEnd: la estancia del informe (stayEnd null si sigue en el centro).
    // lastUpdate: el último apartado guardado o, si no hay, cuándo se creó
    public record ReportSummaryResponse(UUID id, ReportType type, ReportState state, ResidentResponse resident,
                                        LocalDate stayStart, LocalDate stayEnd, Instant lastUpdate,
                                        int writtenSections, int totalSections) {
        static ReportSummaryResponse from(Report report, Resident resident) {
            return new ReportSummaryResponse(report.getId(), report.getType(), report.getState(),
                    ResidentResponse.from(resident), report.getStayStart(), report.getHeader().exitDate(),
                    report.lastUpdate(), report.writtenSections(), report.totalSections());
        }

        public static ReportSummaryResponse from(ReportSummary summary) {
            return from(summary.report(), summary.resident());
        }
    }

    // De la estancia más antigua a la más reciente
    public record ResidentReportsResponse(ResidentResponse resident, List<ReportSummaryResponse> reports) {
        public static ResidentReportsResponse from(ResidentReports view) {
            return new ResidentReportsResponse(ResidentResponse.from(view.resident()),
                    view.reports().stream().map(r -> ReportSummaryResponse.from(r, view.resident())).toList());
        }
    }

    public record HeaderResponse(String requestNumber, String referenceCenter, String nip, LocalDate entryDate,
                                 LocalDate exitDate, String exitReason, LocalDate dueDate, int version) {
        static HeaderResponse from(ReportHeader h, int version) {
            return new HeaderResponse(h.requestNumber(), h.referenceCenter(), h.nip(), h.entryDate(), h.exitDate(),
                    h.exitReason(), h.dueDate(), version);
        }
    }

    // Un apartado, escrito o no. text null y version 0: está vacío. author null: está vacío o quien lo
    // escribió ya no está en la plantilla. adaptation: va dentro del "Grado de adaptación" del área educativa
    public record SectionResponse(ReportSection section, ReportArea area, boolean adaptation, String text,
                                  int version, StaffResponse author, Instant updatedAt) {
        static SectionResponse from(ReportSection section, SectionEntry entry, Map<UUID, StaffMember> staff) {
            if (entry == null) {
                return new SectionResponse(section, section.area(), section.adaptation(), null, 0, null, null);
            }
            return new SectionResponse(section, section.area(), section.adaptation(), entry.text(), entry.version(),
                    entry.author().map(staff::get).map(StaffResponse::from).orElse(null), entry.updatedAt());
        }
    }

    // writers: las especialidades que redactan el área. tutors: también (o solo) los tutores del residente.
    // writable: quien mira puede redactarla. La administración las redacta todas
    public record AreaResponse(ReportArea area, Set<String> writers, boolean tutors, boolean writable) {
    }

    // Todos los apartados del tipo de informe, en orden, escritos o no. authors: "el presente informe ha sido
    // elaborado por los siguientes profesionales". createdBy null: lo creó la aplicación al empezar la estancia.
    // manageable: quien mira puede cerrarlo y reabrirlo
    public record ReportResponse(UUID id, ReportType type, ReportState state, ResidentResponse resident,
                                 LocalDate stayStart, HeaderResponse header, List<AreaResponse> areas,
                                 List<SectionResponse> sections, List<StaffResponse> authors,
                                 StaffResponse createdBy, Instant createdAt, StaffResponse closedBy, Instant closedAt, int writtenSections, int totalSections,
                                 boolean manageable) {
        public static ReportResponse from(ReportView view) {
            Report report = view.report();
            Map<UUID, StaffMember> staff = view.staff();
            ReportType type = report.getType();
            List<AreaResponse> areas = Arrays.stream(ReportArea.values())
                    .filter(area -> area.title(type) != null)
                    .map(area -> new AreaResponse(area, area.writers(), area.writtenByTutors(),
                            view.writableAreas().contains(area)))
                    .toList();
            List<SectionResponse> sections = ReportSection.of(type).stream()
                    .map(section -> SectionResponse.from(section, report.getSection(section).orElse(null), staff))
                    .toList();
            List<StaffResponse> authors = report.authorIds().stream()
                    .map(staff::get).filter(Objects::nonNull).map(StaffResponse::from).toList();
            return new ReportResponse(report.getId(), type, report.getState(),
                    ResidentResponse.from(view.resident()), report.getStayStart(),
                    HeaderResponse.from(report.getHeader(), report.getHeaderVersion()), areas, sections, authors,
                    report.getCreatedBy().map(staff::get).map(StaffResponse::from).orElse(null),
                    report.getCreatedAt(),
                    report.getClosedBy().map(staff::get).map(StaffResponse::from).orElse(null),
                    report.getClosedAt().orElse(null), report.writtenSections(), report.totalSections(),
                    view.manageable());
        }
    }
}
