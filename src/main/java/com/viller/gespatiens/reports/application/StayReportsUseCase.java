package com.viller.gespatiens.reports.application;

import com.viller.gespatiens.reports.domain.Report;
import com.viller.gespatiens.reports.domain.ReportRepository;
import com.viller.gespatiens.reports.domain.ReportType;
import com.viller.gespatiens.reports.domain.Resident;
import com.viller.gespatiens.reports.domain.ResidentDirectory;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// Cada estancia tiene su informe de media estancia y su informe final, vacíos desde el primer día: "cada vez
// que entre alguien se debería crear un informe vacío" (decisión del centro). Se crean al dar de alta o
// reincorporar al residente; al darle de baja, su fecha de salida pasa a la cabecera. Por si un aviso se pierde
// (o el residente entró antes de existir los informes), las pantallas de informes vuelven a comprobarlo:
// crear lo que falte es idempotente
public class StayReportsUseCase {

    private final ReportRepository reportRepository;
    private final ResidentDirectory residentDirectory;
    private final Clock clock;

    public StayReportsUseCase(ReportRepository reportRepository, ResidentDirectory residentDirectory, Clock clock) {
        this.reportRepository = reportRepository;
        this.residentDirectory = residentDirectory;
        this.clock = clock;
    }

    // Al dar de alta, reincorporar o dar de baja: la ficha ya está guardada, así que basta con ponerse al día
    public void stayChanged(UUID patientId) {
        residentDirectory.find(patientId).ifPresent(this::ensureCurrentStay);
    }

    // Los informes de la estancia actual (o la última, si ya se fue), creando los que falten.
    // Devuelve todos los informes del residente, del más antiguo al más reciente
    public List<Report> ensureCurrentStay(Resident resident) {
        LocalDate stayStart = resident.entryDate();
        List<Report> existing = reportRepository.findByPatient(resident.id());
        boolean created = false;
        for (ReportType type : ReportType.values()) {
            boolean missing = existing.stream()
                    .noneMatch(r -> r.getType() == type && r.getStayStart().equals(stayStart));
            if (missing) {
                reportRepository.addIfMissing(Report.forStay(resident.id(), type, stayStart, clock.instant()));
                created = true;
            }
        }
        if (!resident.active()) {
            created |= recordStayEnd(resident.id(), stayStart, resident.exitDate());
        }
        return created ? reportRepository.findByPatient(resident.id()) : existing;
    }

    private boolean recordStayEnd(UUID patientId, LocalDate stayStart, LocalDate exitDate) {
        boolean changed = false;
        for (Report report : reportRepository.findByPatient(patientId)) {
            if (report.getStayStart().equals(stayStart) && report.recordStayEnd(exitDate)) {
                reportRepository.save(report);
                changed = true;
            }
        }
        return changed;
    }
}
