package com.viller.gespatiens.reports.infrastructure.web;

import com.viller.gespatiens.reports.application.CloseReportUseCase;
import com.viller.gespatiens.reports.application.EditReportHeaderUseCase;
import com.viller.gespatiens.reports.application.ExportReportsPdfUseCase;
import com.viller.gespatiens.reports.application.GetReportUseCase;
import com.viller.gespatiens.reports.application.ListReportsUseCase;
import com.viller.gespatiens.reports.application.ListResidentReportsUseCase;
import com.viller.gespatiens.reports.application.PdfFile;
import com.viller.gespatiens.reports.application.WriteSectionUseCase;
import com.viller.gespatiens.reports.domain.ReportSection;
import com.viller.gespatiens.reports.domain.ReportType;
import com.viller.gespatiens.reports.infrastructure.web.ReportRequests.HeaderRequest;
import com.viller.gespatiens.reports.infrastructure.web.ReportRequests.SectionRequest;
import com.viller.gespatiens.reports.infrastructure.web.ReportResponses.ReportResponse;
import com.viller.gespatiens.reports.infrastructure.web.ReportResponses.ReportSummaryResponse;
import com.viller.gespatiens.reports.infrastructure.web.ReportResponses.ResidentReportsResponse;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

// Los informes de media estancia y de fin de programa (RF1, RF2, RF8). Se crean solos con cada estancia y todo
// el personal los consulta; qué área redacta cada uno, y que solo la administración los cierre y reabra, lo
// decide el dominio (y SecurityConfig, para cerrar y reabrir)
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ListReportsUseCase listReportsUseCase;
    private final ListResidentReportsUseCase listResidentReportsUseCase;
    private final GetReportUseCase getReportUseCase;
    private final WriteSectionUseCase writeSectionUseCase;
    private final EditReportHeaderUseCase editReportHeaderUseCase;
    private final CloseReportUseCase closeReportUseCase;
    private final ExportReportsPdfUseCase exportReportsPdfUseCase;

    public ReportController(ListReportsUseCase listReportsUseCase,
                            ListResidentReportsUseCase listResidentReportsUseCase,
                            GetReportUseCase getReportUseCase,
                            WriteSectionUseCase writeSectionUseCase,
                            EditReportHeaderUseCase editReportHeaderUseCase,
                            CloseReportUseCase closeReportUseCase,
                            ExportReportsPdfUseCase exportReportsPdfUseCase) {
        this.listReportsUseCase = listReportsUseCase;
        this.listResidentReportsUseCase = listResidentReportsUseCase;
        this.getReportUseCase = getReportUseCase;
        this.writeSectionUseCase = writeSectionUseCase;
        this.editReportHeaderUseCase = editReportHeaderUseCase;
        this.closeReportUseCase = closeReportUseCase;
        this.exportReportsPdfUseCase = exportReportsPdfUseCase;
    }

    @GetMapping
    public List<ReportSummaryResponse> list() {
        return listReportsUseCase.execute().stream().map(ReportSummaryResponse::from).toList();
    }

    @GetMapping("/residents/{patientId}")
    public ResidentReportsResponse ofResident(@PathVariable UUID patientId) {
        return ResidentReportsResponse.from(listResidentReportsUseCase.execute(patientId));
    }

    // RF2: todos los informes de un residente en un único PDF; ?type=MID_STAY o FINAL para uno de los tipos.
    // ?inline=true para verlo en el navegador (vista previa) en lugar de descargarlo
    @GetMapping(value = "/residents/{patientId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> residentPdf(@PathVariable UUID patientId,
                                              @RequestParam(required = false) ReportType type,
                                              @RequestParam(defaultValue = "false") boolean inline) {
        return pdf(exportReportsPdfUseCase.ofResident(patientId, type), inline);
    }

    @GetMapping("/{id}")
    public ReportResponse get(Principal principal, @PathVariable UUID id) {
        return ReportResponse.from(getReportUseCase.execute(id, currentUserId(principal)));
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> reportPdf(@PathVariable UUID id,
                                            @RequestParam(defaultValue = "false") boolean inline) {
        return pdf(exportReportsPdfUseCase.one(id), inline);
    }

    @PutMapping("/{id}/sections/{section}")
    public ReportResponse writeSection(Principal principal, @PathVariable UUID id,
                                       @PathVariable ReportSection section, @RequestBody SectionRequest request) {
        return ReportResponse.from(writeSectionUseCase.execute(id, section, request.text(), request.version(),
                currentUserId(principal)));
    }

    @PutMapping("/{id}/header")
    public ReportResponse editHeader(Principal principal, @PathVariable UUID id,
                                     @RequestBody HeaderRequest request) {
        return ReportResponse.from(editReportHeaderUseCase.execute(id, request.requestNumber(),
                request.referenceCenter(), request.nip(), request.exitReason(), request.version(),
                currentUserId(principal)));
    }

    // Cerrar no borra nada, cambia el estado. Por eso es un POST a una acción
    @PostMapping("/{id}/close")
    public ReportResponse close(Principal principal, @PathVariable UUID id) {
        return ReportResponse.from(closeReportUseCase.close(id, currentUserId(principal)));
    }

    @PostMapping("/{id}/reopen")
    public ReportResponse reopen(Principal principal, @PathVariable UUID id) {
        return ReportResponse.from(closeReportUseCase.reopen(id, currentUserId(principal)));
    }

    // attachment: el navegador lo descarga con su nombre. inline: lo abre en la pestaña (vista previa)
    private static ResponseEntity<byte[]> pdf(PdfFile file, boolean inline) {
        ContentDisposition disposition = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(file.fileName()).build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file.content());
    }

    // CurrentUserJwtConverter pone el id del empleado como "name" de la autenticación
    private static UUID currentUserId(Principal principal) {
        return UUID.fromString(principal.getName());
    }
}
