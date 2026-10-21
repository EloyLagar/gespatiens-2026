package com.viller.gespatiens.reports.application;

import com.viller.gespatiens.reports.domain.Report;
import com.viller.gespatiens.reports.domain.ReportArea;
import com.viller.gespatiens.reports.domain.ReportNotFoundException;
import com.viller.gespatiens.reports.domain.ReportRepository;
import com.viller.gespatiens.reports.domain.ReportType;
import com.viller.gespatiens.reports.domain.Resident;
import com.viller.gespatiens.reports.domain.ResidentDirectory;
import com.viller.gespatiens.reports.domain.ResidentNotFoundException;
import com.viller.gespatiens.reports.domain.StaffDirectory;
import com.viller.gespatiens.reports.domain.StaffMember;
import com.viller.gespatiens.reports.domain.TutorDirectory;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Arma las vistas del módulo y busca lo que todos los casos de uso necesitan
final class ReportViews {

    private ReportViews() {
    }

    static Report report(ReportRepository reports, UUID reportId) {
        return reports.findById(reportId).orElseThrow(() -> new ReportNotFoundException(reportId));
    }

    static Resident resident(ResidentDirectory residents, UUID patientId) {
        return residents.find(patientId).orElseThrow(() -> new ResidentNotFoundException(patientId));
    }

    // Quien hace la petición. Con un token válido siempre existe, salvo que lo acaben de borrar
    static StaffMember member(StaffDirectory staff, UUID employeeId) {
        return staff.find(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("No existe ningún empleado con id " + employeeId));
    }

    static ReportView of(Report report, Resident resident, StaffDirectory staff, TutorDirectory tutors,
                         StaffMember viewer) {
        List<UUID> people = Stream.of(report.authorIds().stream(), report.getCreatedBy().stream(),
                report.getClosedBy().stream()).flatMap(s -> s).distinct().toList();
        Map<UUID, StaffMember> found = staff.findAll(people);
        boolean tutor = tutors.isTutor(report.getPatientId(), viewer.id());
        return new ReportView(report, resident, found, writableAreas(report.getType(), viewer, tutor),
                viewer.administrator());
    }

    private static Set<ReportArea> writableAreas(ReportType type, StaffMember viewer, boolean tutor) {
        return Arrays.stream(ReportArea.values())
                .filter(area -> area.title(type) != null && area.canBeWrittenBy(viewer, tutor))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ReportArea.class)));
    }
}
