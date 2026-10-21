package com.viller.gespatiens.reports.application;

import com.viller.gespatiens.reports.domain.EditingPresence;
import com.viller.gespatiens.reports.domain.Report;
import com.viller.gespatiens.reports.domain.ReportRepository;
import com.viller.gespatiens.reports.domain.ReportSection;
import com.viller.gespatiens.reports.domain.ResidentDirectory;
import com.viller.gespatiens.reports.domain.StaffDirectory;
import com.viller.gespatiens.reports.domain.StaffMember;
import com.viller.gespatiens.reports.domain.TutorDirectory;

import java.time.Clock;
import java.util.UUID;

// Redactar un apartado del informe (RF1 "aportar a estos"). RF8: si otra persona lo está editando no se
// guarda; si lo cambió mientras tanto, tampoco (lo comprueba la versión)
public class WriteSectionUseCase {

    private final ReportRepository reportRepository;
    private final ResidentDirectory residentDirectory;
    private final StaffDirectory staffDirectory;
    private final TutorDirectory tutorDirectory;
    private final EditingPresence editingPresence;
    private final Clock clock;

    public WriteSectionUseCase(ReportRepository reportRepository, ResidentDirectory residentDirectory,
                               StaffDirectory staffDirectory, TutorDirectory tutorDirectory,
                               EditingPresence editingPresence, Clock clock) {
        this.reportRepository = reportRepository;
        this.residentDirectory = residentDirectory;
        this.staffDirectory = staffDirectory;
        this.tutorDirectory = tutorDirectory;
        this.editingPresence = editingPresence;
        this.clock = clock;
    }

    public ReportView execute(UUID reportId, ReportSection section, String text, int expectedVersion,
                              UUID writerId) {
        Report report = ReportViews.report(reportRepository, reportId);
        StaffMember writer = ReportViews.member(staffDirectory, writerId);
        if (section != null) {
            editingPresence.ensureNotEditedByOther(EditingPresence.section(reportId, section), writerId);
        }
        report.write(section, text, expectedVersion, writer,
                tutorDirectory.isTutor(report.getPatientId(), writerId), clock.instant());
        Report saved = reportRepository.save(report);
        return ReportViews.of(saved, ReportViews.resident(residentDirectory, saved.getPatientId()), staffDirectory,
                tutorDirectory, writer);
    }
}
