package com.viller.gespatiens.reports.application;

import com.viller.gespatiens.reports.domain.Report;
import com.viller.gespatiens.reports.domain.ReportRepository;
import com.viller.gespatiens.reports.domain.ResidentDirectory;
import com.viller.gespatiens.reports.domain.StaffDirectory;
import com.viller.gespatiens.reports.domain.StaffMember;
import com.viller.gespatiens.reports.domain.TutorDirectory;

import java.time.Clock;
import java.util.UUID;

// Cerrar un informe terminado, para que nadie lo cambie por error, o reabrirlo para corregirlo. Solo la
// administración
public class CloseReportUseCase {

    private final ReportRepository reportRepository;
    private final ResidentDirectory residentDirectory;
    private final StaffDirectory staffDirectory;
    private final TutorDirectory tutorDirectory;
    private final Clock clock;

    public CloseReportUseCase(ReportRepository reportRepository, ResidentDirectory residentDirectory,
                              StaffDirectory staffDirectory, TutorDirectory tutorDirectory, Clock clock) {
        this.reportRepository = reportRepository;
        this.residentDirectory = residentDirectory;
        this.staffDirectory = staffDirectory;
        this.tutorDirectory = tutorDirectory;
        this.clock = clock;
    }

    public ReportView close(UUID reportId, UUID adminId) {
        Report report = ReportViews.report(reportRepository, reportId);
        StaffMember admin = ReportViews.member(staffDirectory, adminId);
        report.close(admin, clock.instant());
        return save(report, admin);
    }

    public ReportView reopen(UUID reportId, UUID adminId) {
        Report report = ReportViews.report(reportRepository, reportId);
        StaffMember admin = ReportViews.member(staffDirectory, adminId);
        report.reopen(admin);
        return save(report, admin);
    }

    private ReportView save(Report report, StaffMember admin) {
        Report saved = reportRepository.save(report);
        return ReportViews.of(saved, ReportViews.resident(residentDirectory, saved.getPatientId()), staffDirectory,
                tutorDirectory, admin);
    }
}
