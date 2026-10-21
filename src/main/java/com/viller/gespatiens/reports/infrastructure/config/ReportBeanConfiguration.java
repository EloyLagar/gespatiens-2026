package com.viller.gespatiens.reports.infrastructure.config;

import com.viller.gespatiens.reports.application.CloseReportUseCase;
import com.viller.gespatiens.reports.application.EditReportHeaderUseCase;
import com.viller.gespatiens.reports.application.ExportReportsPdfUseCase;
import com.viller.gespatiens.reports.application.GetReportUseCase;
import com.viller.gespatiens.reports.application.ListReportsUseCase;
import com.viller.gespatiens.reports.application.ListResidentReportsUseCase;
import com.viller.gespatiens.reports.application.ReportPdfRenderer;
import com.viller.gespatiens.reports.application.StayReportsUseCase;
import com.viller.gespatiens.reports.application.WriteSectionUseCase;
import com.viller.gespatiens.reports.domain.EditingPresence;
import com.viller.gespatiens.reports.domain.ReportRepository;
import com.viller.gespatiens.reports.domain.ResidentDirectory;
import com.viller.gespatiens.reports.domain.SignatureDirectory;
import com.viller.gespatiens.reports.domain.StaffDirectory;
import com.viller.gespatiens.reports.domain.TutorDirectory;
import com.viller.gespatiens.reports.infrastructure.pdf.OpenPdfReportRenderer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

// Los casos de uso no llevan anotaciones de Spring: se registran aquí. El Clock lo define NoticeBeanConfiguration
@Configuration
public class ReportBeanConfiguration {

    @Bean
    public ReportPdfRenderer reportPdfRenderer() {
        return new OpenPdfReportRenderer();
    }

    @Bean
    public StayReportsUseCase stayReportsUseCase(ReportRepository reportRepository,
                                                 ResidentDirectory reportsResidentDirectory, Clock clock) {
        return new StayReportsUseCase(reportRepository, reportsResidentDirectory, clock);
    }

    @Bean
    public ListReportsUseCase listReportsUseCase(ReportRepository reportRepository,
                                                 ResidentDirectory reportsResidentDirectory,
                                                 StayReportsUseCase stayReportsUseCase) {
        return new ListReportsUseCase(reportRepository, reportsResidentDirectory, stayReportsUseCase);
    }

    @Bean
    public ListResidentReportsUseCase listResidentReportsUseCase(StayReportsUseCase stayReportsUseCase,
                                                                 ResidentDirectory reportsResidentDirectory) {
        return new ListResidentReportsUseCase(stayReportsUseCase, reportsResidentDirectory);
    }

    @Bean
    public GetReportUseCase getReportUseCase(ReportRepository reportRepository,
                                             ResidentDirectory reportsResidentDirectory,
                                             StaffDirectory reportsStaffDirectory,
                                             TutorDirectory reportsTutorDirectory) {
        return new GetReportUseCase(reportRepository, reportsResidentDirectory, reportsStaffDirectory,
                reportsTutorDirectory);
    }

    @Bean
    public WriteSectionUseCase writeSectionUseCase(ReportRepository reportRepository,
                                                   ResidentDirectory reportsResidentDirectory,
                                                   StaffDirectory reportsStaffDirectory,
                                                   TutorDirectory reportsTutorDirectory,
                                                   EditingPresence reportsEditingPresence, Clock clock) {
        return new WriteSectionUseCase(reportRepository, reportsResidentDirectory, reportsStaffDirectory,
                reportsTutorDirectory, reportsEditingPresence, clock);
    }

    @Bean
    public EditReportHeaderUseCase editReportHeaderUseCase(ReportRepository reportRepository,
                                                           ResidentDirectory reportsResidentDirectory,
                                                           StaffDirectory reportsStaffDirectory,
                                                           TutorDirectory reportsTutorDirectory,
                                                           EditingPresence reportsEditingPresence) {
        return new EditReportHeaderUseCase(reportRepository, reportsResidentDirectory, reportsStaffDirectory,
                reportsTutorDirectory, reportsEditingPresence);
    }

    @Bean
    public CloseReportUseCase closeReportUseCase(ReportRepository reportRepository,
                                                 ResidentDirectory reportsResidentDirectory,
                                                 StaffDirectory reportsStaffDirectory,
                                                 TutorDirectory reportsTutorDirectory, Clock clock) {
        return new CloseReportUseCase(reportRepository, reportsResidentDirectory, reportsStaffDirectory,
                reportsTutorDirectory, clock);
    }

    @Bean
    public ExportReportsPdfUseCase exportReportsPdfUseCase(ReportRepository reportRepository,
                                                           ResidentDirectory reportsResidentDirectory,
                                                           StaffDirectory reportsStaffDirectory,
                                                           SignatureDirectory reportsSignatureDirectory,
                                                           ReportPdfRenderer reportPdfRenderer) {
        return new ExportReportsPdfUseCase(reportRepository, reportsResidentDirectory, reportsStaffDirectory,
                reportsSignatureDirectory, reportPdfRenderer);
    }
}
