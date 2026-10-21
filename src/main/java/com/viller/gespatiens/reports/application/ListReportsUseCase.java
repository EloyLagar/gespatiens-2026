package com.viller.gespatiens.reports.application;

import com.viller.gespatiens.reports.domain.Report;
import com.viller.gespatiens.reports.domain.ReportRepository;
import com.viller.gespatiens.reports.domain.Resident;
import com.viller.gespatiens.reports.domain.ResidentDirectory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// El apartado "Informes": los de todos los residentes, del que se tocó más recientemente al que menos.
// Los de quienes están en el centro siempre aparecen, aunque nadie los haya abierto todavía
public class ListReportsUseCase {

    private final ReportRepository reportRepository;
    private final ResidentDirectory residentDirectory;
    private final StayReportsUseCase stayReports;

    public ListReportsUseCase(ReportRepository reportRepository, ResidentDirectory residentDirectory,
                              StayReportsUseCase stayReports) {
        this.reportRepository = reportRepository;
        this.residentDirectory = residentDirectory;
        this.stayReports = stayReports;
    }

    public List<ReportSummary> execute() {
        // Como mucho 32 residentes: comprobar sus informes uno a uno es barato
        residentDirectory.findActive().forEach(stayReports::ensureCurrentStay);
        List<Report> reports = reportRepository.findAll();
        Map<UUID, Resident> residents = residentDirectory.findAll(
                reports.stream().map(Report::getPatientId).distinct().toList());
        // Si se borra al residente se borran sus informes, así que siempre lo encuentra
        return reports.stream()
                .filter(report -> residents.containsKey(report.getPatientId()))
                .sorted(Comparator.comparing(Report::lastUpdate).reversed())
                .map(report -> new ReportSummary(report, residents.get(report.getPatientId())))
                .toList();
    }
}
