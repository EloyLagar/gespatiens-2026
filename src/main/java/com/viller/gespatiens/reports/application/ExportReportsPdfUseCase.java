package com.viller.gespatiens.reports.application;

import com.viller.gespatiens.reports.application.PrintableReport.Signer;
import com.viller.gespatiens.reports.domain.Report;
import com.viller.gespatiens.reports.domain.ReportRepository;
import com.viller.gespatiens.reports.domain.ReportType;
import com.viller.gespatiens.reports.domain.Resident;
import com.viller.gespatiens.reports.domain.ResidentDirectory;
import com.viller.gespatiens.reports.domain.SignatureDirectory;
import com.viller.gespatiens.reports.domain.StaffDirectory;
import com.viller.gespatiens.reports.domain.StaffMember;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

// Los informes en PDF "con el estilo y cabeceras que han sido aportadas por el centro": uno solo, o todos los
// de un residente en un único documento (RF2): los de un tipo de todas sus estancias, o los dos tipos juntos
public class ExportReportsPdfUseCase {

    private final ReportRepository reportRepository;
    private final ResidentDirectory residentDirectory;
    private final StaffDirectory staffDirectory;
    private final SignatureDirectory signatureDirectory;
    private final ReportPdfRenderer renderer;

    public ExportReportsPdfUseCase(ReportRepository reportRepository, ResidentDirectory residentDirectory,
                                   StaffDirectory staffDirectory, SignatureDirectory signatureDirectory,
                                   ReportPdfRenderer renderer) {
        this.reportRepository = reportRepository;
        this.residentDirectory = residentDirectory;
        this.staffDirectory = staffDirectory;
        this.signatureDirectory = signatureDirectory;
        this.renderer = renderer;
    }

    public PdfFile one(UUID reportId) {
        Report report = ReportViews.report(reportRepository, reportId);
        Resident resident = ReportViews.resident(residentDirectory, report.getPatientId());
        return new PdfFile(fileName(report.getType().title(), resident),
                renderer.render(List.of(printable(report, resident))));
    }

    // RF2: type null reúne los dos tipos. Por estancia, de la más antigua a la más reciente, y dentro de cada
    // estancia primero el de media estancia
    public PdfFile ofResident(UUID patientId, ReportType type) {
        Resident resident = ReportViews.resident(residentDirectory, patientId);
        List<PrintableReport> printable = reportRepository.findByPatient(patientId).stream()
                .filter(report -> type == null || report.getType() == type)
                .sorted(Comparator.comparing(Report::getStayStart).thenComparing(Report::getType))
                .map(report -> printable(report, resident))
                .toList();
        if (printable.isEmpty()) {
            throw new IllegalArgumentException("El residente no tiene ningún informe que reunir");
        }
        String title = type == null ? "Informes" : "Informes " + type.title().substring("Informe ".length());
        return new PdfFile(fileName(title, resident), renderer.render(printable));
    }

    private PrintableReport printable(Report report, Resident resident) {
        Map<UUID, StaffMember> staff = staffDirectory.findAll(report.authorIds());
        List<Signer> authors = report.authorIds().stream()
                .map(staff::get)
                .filter(Objects::nonNull)
                .map(member -> new Signer(member, signatureDirectory.signatureOf(member.id())))
                .toList();
        return new PrintableReport(report, resident, authors);
    }

    // "informe-de-media-estancia-martinez-lucia.pdf": sin tildes ni espacios, que algunos sistemas no los aceptan
    static String fileName(String title, Resident resident) {
        String raw = title + " " + resident.surname() + " " + resident.name();
        String ascii = Normalizer.normalize(raw, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return ascii.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "") + ".pdf";
    }
}
