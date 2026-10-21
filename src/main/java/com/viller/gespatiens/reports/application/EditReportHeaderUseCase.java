package com.viller.gespatiens.reports.application;

import com.viller.gespatiens.reports.domain.EditingPresence;
import com.viller.gespatiens.reports.domain.Report;
import com.viller.gespatiens.reports.domain.ReportRepository;
import com.viller.gespatiens.reports.domain.ResidentDirectory;
import com.viller.gespatiens.reports.domain.StaffDirectory;
import com.viller.gespatiens.reports.domain.StaffMember;
import com.viller.gespatiens.reports.domain.TutorDirectory;

import java.time.LocalDate;
import java.util.UUID;

// Completar la cabecera: nº de solicitud, centro de referencia, N.I.P., motivo de salida y, en el de media
// estancia, cuándo toca
public class EditReportHeaderUseCase {

    private final ReportRepository reportRepository;
    private final ResidentDirectory residentDirectory;
    private final StaffDirectory staffDirectory;
    private final TutorDirectory tutorDirectory;
    private final EditingPresence editingPresence;

    public EditReportHeaderUseCase(ReportRepository reportRepository, ResidentDirectory residentDirectory,
                                   StaffDirectory staffDirectory, TutorDirectory tutorDirectory,
                                   EditingPresence editingPresence) {
        this.reportRepository = reportRepository;
        this.residentDirectory = residentDirectory;
        this.staffDirectory = staffDirectory;
        this.tutorDirectory = tutorDirectory;
        this.editingPresence = editingPresence;
    }

    public ReportView execute(UUID reportId, String requestNumber, String referenceCenter, String nip,
                              String exitReason, LocalDate dueDate, int expectedVersion, UUID editorId) {
        Report report = ReportViews.report(reportRepository, reportId);
        StaffMember editor = ReportViews.member(staffDirectory, editorId);
        editingPresence.ensureNotEditedByOther(EditingPresence.header(reportId), editorId);
        report.editHeader(requestNumber, referenceCenter, nip, exitReason, dueDate, expectedVersion);
        Report saved = reportRepository.save(report);
        return ReportViews.of(saved, ReportViews.resident(residentDirectory, saved.getPatientId()), staffDirectory,
                tutorDirectory, editor);
    }
}
