package com.viller.gespatiens.reports.application;

import com.viller.gespatiens.reports.domain.Report;
import com.viller.gespatiens.reports.domain.ReportRepository;
import com.viller.gespatiens.reports.domain.ResidentDirectory;
import com.viller.gespatiens.reports.domain.StaffDirectory;
import com.viller.gespatiens.reports.domain.TutorDirectory;

import java.util.UUID;

// Abrir un informe. Todo el personal lo lee; lo que puede redactar cada uno lo dice la vista
public class GetReportUseCase {

    private final ReportRepository reportRepository;
    private final ResidentDirectory residentDirectory;
    private final StaffDirectory staffDirectory;
    private final TutorDirectory tutorDirectory;

    public GetReportUseCase(ReportRepository reportRepository, ResidentDirectory residentDirectory,
                            StaffDirectory staffDirectory, TutorDirectory tutorDirectory) {
        this.reportRepository = reportRepository;
        this.residentDirectory = residentDirectory;
        this.staffDirectory = staffDirectory;
        this.tutorDirectory = tutorDirectory;
    }

    public ReportView execute(UUID reportId, UUID viewerId) {
        Report report = ReportViews.report(reportRepository, reportId);
        return ReportViews.of(report, ReportViews.resident(residentDirectory, report.getPatientId()),
                staffDirectory, tutorDirectory, ReportViews.member(staffDirectory, viewerId));
    }
}
