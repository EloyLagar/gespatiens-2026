package com.viller.gespatiens.reports.infrastructure.web;

import com.viller.gespatiens.access.users.infrastructure.security.CurrentUserJwtConverter;
import com.viller.gespatiens.access.users.infrastructure.security.SecurityConfig;
import com.viller.gespatiens.reports.application.CloseReportUseCase;
import com.viller.gespatiens.reports.application.EditReportHeaderUseCase;
import com.viller.gespatiens.reports.application.ExportReportsPdfUseCase;
import com.viller.gespatiens.reports.application.GetReportUseCase;
import com.viller.gespatiens.reports.application.ListReportsUseCase;
import com.viller.gespatiens.reports.application.ListResidentReportsUseCase;
import com.viller.gespatiens.reports.application.PdfFile;
import com.viller.gespatiens.reports.application.ReportView;
import com.viller.gespatiens.reports.application.ResidentReports;
import com.viller.gespatiens.reports.application.WriteSectionUseCase;
import com.viller.gespatiens.editing.domain.ResourceBeingEditedException;
import com.viller.gespatiens.reports.domain.AreaNotWritableException;
import com.viller.gespatiens.reports.domain.Report;
import com.viller.gespatiens.reports.domain.ReportArea;
import com.viller.gespatiens.reports.domain.ReportSection;
import com.viller.gespatiens.reports.domain.ReportType;
import com.viller.gespatiens.reports.domain.Resident;
import com.viller.gespatiens.reports.domain.SectionConflictException;
import com.viller.gespatiens.reports.domain.SectionEntry;
import com.viller.gespatiens.reports.domain.StaffMember;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@Import(SecurityConfig.class)
class ReportControllerTest {

    private static final String ME = "7f3c2a10-0000-4000-8000-0000000000aa";
    private static final UUID ME_ID = UUID.fromString(ME);
    private static final UUID PATIENT = UUID.fromString("7f3c2a10-0000-4000-8000-000000000001");
    private static final Instant NOW = Instant.parse("2026-10-21T10:00:00Z");

    private final Resident lucia = new Resident(PATIENT, 5, "Lucía", "Martínez", "12345678Z",
            LocalDate.of(1990, 5, 3), null, LocalDate.of(2026, 4, 1), null, true);
    private final StaffMember marcos = new StaffMember(ME_ID, "Marcos", "PSYCHOLOGIST", false);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListReportsUseCase listReportsUseCase;
    @MockitoBean
    private ListResidentReportsUseCase listResidentReportsUseCase;
    @MockitoBean
    private GetReportUseCase getReportUseCase;
    @MockitoBean
    private WriteSectionUseCase writeSectionUseCase;
    @MockitoBean
    private EditReportHeaderUseCase editReportHeaderUseCase;
    @MockitoBean
    private CloseReportUseCase closeReportUseCase;
    @MockitoBean
    private ExportReportsPdfUseCase exportReportsPdfUseCase;

    // La seguridad real necesita estos dos beans; con @WithMockUser no se llegan a usar
    @MockitoBean
    private JwtDecoder jwtDecoder;
    @MockitoBean
    private CurrentUserJwtConverter currentUserJwtConverter;

    private ReportView view(Report report) {
        return new ReportView(report, lucia, Map.of(ME_ID, marcos), Set.of(ReportArea.PSYCHOLOGICAL), false);
    }

    private Report midStay() {
        Report report = Report.forStay(PATIENT, ReportType.MID_STAY, LocalDate.of(2026, 4, 1), NOW);
        report.write(ReportSection.PSYCHO_DIAGNOSIS, "Dual", 0, marcos, false, NOW);
        return report;
    }

    @Test
    @WithMockUser(username = ME, roles = "PSYCHOLOGIST")
    void aReportComesWithEverySectionOfItsTypeAndWhatTheViewerCanWrite() throws Exception {
        Report report = midStay();
        when(getReportUseCase.execute(report.getId(), ME_ID)).thenReturn(view(report));

        mockMvc.perform(get("/api/reports/{id}", report.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("MID_STAY"))
                .andExpect(jsonPath("$.state").value("DRAFT"))
                .andExpect(jsonPath("$.resident.dni").value("12345678Z"))
                .andExpect(jsonPath("$.sections.length()").value(ReportSection.of(ReportType.MID_STAY).size()))
                .andExpect(jsonPath("$.sections[?(@.section=='PSYCHO_DIAGNOSIS')].text").value("Dual"))
                .andExpect(jsonPath("$.sections[?(@.section=='PSYCHO_DIAGNOSIS')].author.name").value("Marcos"))
                .andExpect(jsonPath("$.sections[0].version").value(0))
                .andExpect(jsonPath("$.areas[?(@.area=='PSYCHOLOGICAL')].writable").value(true))
                .andExpect(jsonPath("$.areas[?(@.area=='HEALTH')].writable").value(false))
                .andExpect(jsonPath("$.areas.length()").value(6))
                .andExpect(jsonPath("$.authors[0].name").value("Marcos"))
                .andExpect(jsonPath("$.manageable").value(false));
    }

    @Test
    @WithMockUser(username = ME, roles = "PSYCHOLOGIST")
    void theResidentReportsComeWithTheirStay() throws Exception {
        when(listResidentReportsUseCase.execute(PATIENT)).thenReturn(
                new ResidentReports(lucia, List.of(midStay())));

        mockMvc.perform(get("/api/reports/residents/{id}", PATIENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reports[0].writtenSections").value(1))
                .andExpect(jsonPath("$.reports[0].stayStart").value("2026-04-01"))
                .andExpect(jsonPath("$.reports[0].stayEnd").doesNotExist());
    }

    // RF8: el 409 trae lo que hay ahora en el apartado para que quien editaba lo compare
    @Test
    @WithMockUser(username = ME, roles = "PSYCHOLOGIST")
    void aConflictingSaveReturnsTheCurrentText() throws Exception {
        UUID id = UUID.randomUUID();
        when(writeSectionUseCase.execute(id, ReportSection.PSYCHO_DIAGNOSIS, "Mía", 1, ME_ID)).thenThrow(
                new SectionConflictException(ReportSection.PSYCHO_DIAGNOSIS,
                        new SectionEntry("Suya", UUID.randomUUID(), NOW, 2)));

        mockMvc.perform(put("/api/reports/{id}/sections/PSYCHO_DIAGNOSIS", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Mía\",\"version\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.section").value("PSYCHO_DIAGNOSIS"))
                .andExpect(jsonPath("$.currentText").value("Suya"))
                .andExpect(jsonPath("$.currentVersion").value(2));
    }

    @Test
    @WithMockUser(username = ME, roles = "SOCIAL_EDUCATOR")
    void writingAnotherSpecialitysAreaIsForbidden() throws Exception {
        UUID id = UUID.randomUUID();
        when(writeSectionUseCase.execute(id, ReportSection.HEALTH_AT_ENTRY, "Estable", 0, ME_ID))
                .thenThrow(new AreaNotWritableException(ReportArea.HEALTH, ReportType.MID_STAY));

        mockMvc.perform(put("/api/reports/{id}/sections/HEALTH_AT_ENTRY", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Estable\",\"version\":0}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = ME, roles = "PSYCHOLOGIST")
    void anUnknownSectionIsABadRequest() throws Exception {
        mockMvc.perform(put("/api/reports/{id}/sections/NOPE", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"x\",\"version\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = ME, roles = "PSYCHOLOGIST")
    void onlyAdministrationClosesAndReopens() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/api/reports/{id}/close", id)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/reports/{id}/reopen", id)).andExpect(status().isForbidden());
        verifyNoInteractions(closeReportUseCase);
    }

    // Los informes van con la estancia: no se borran ni se empiezan a mano
    @Test
    @WithMockUser(username = ME, roles = "ADMINISTRATOR")
    void reportsAreNeitherDeletedNorStartedByHand() throws Exception {
        mockMvc.perform(delete("/api/reports/{id}", UUID.randomUUID())).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(post("/api/reports/residents/{id}", PATIENT)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"FINAL\"}"))
                .andExpect(status().isMethodNotAllowed());
    }

    // RF8: si otra persona tiene el apartado abierto, se dice quién
    @Test
    @WithMockUser(username = ME, roles = "PSYCHOLOGIST")
    void aSectionBeingEditedBySomeoneElseIsAConflict() throws Exception {
        UUID id = UUID.randomUUID();
        when(writeSectionUseCase.execute(id, ReportSection.PSYCHO_DIAGNOSIS, "Mía", 1, ME_ID)).thenThrow(
                new ResourceBeingEditedException("Elena", NOW));

        mockMvc.perform(put("/api/reports/{id}/sections/PSYCHO_DIAGNOSIS", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Mía\",\"version\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.beingEditedBy").value("Elena"))
                .andExpect(jsonPath("$.since").value("2026-10-21T10:00:00Z"));
    }

    @Test
    @WithMockUser(username = ME, roles = "ADMINISTRATOR")
    void administrationClosesAReport() throws Exception {
        Report report = midStay();
        StaffMember rosa = new StaffMember(ME_ID, "Rosa", "ADMINISTRATOR", true);
        report.close(rosa, NOW);
        when(closeReportUseCase.close(report.getId(), ME_ID)).thenReturn(
                new ReportView(report, lucia, Map.of(ME_ID, rosa), Set.of(), true));

        mockMvc.perform(post("/api/reports/{id}/close", report.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("CLOSED"))
                .andExpect(jsonPath("$.closedBy.name").value("Rosa"))
                .andExpect(jsonPath("$.manageable").value(true));
    }

    // RF2: el PDF se descarga con su nombre
    @Test
    @WithMockUser(username = ME, roles = "SOCIAL_WORKER")
    void theResidentPdfIsDownloadedAsAnAttachment() throws Exception {
        when(exportReportsPdfUseCase.ofResident(PATIENT, ReportType.MID_STAY)).thenReturn(
                new PdfFile("informe-de-media-estancia-martinez-lucia.pdf", new byte[]{37, 80, 68, 70}));

        mockMvc.perform(get("/api/reports/residents/{id}/pdf", PATIENT).param("type", "MID_STAY"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"informe-de-media-estancia-martinez-lucia.pdf\""))
                .andExpect(content().bytes(new byte[]{37, 80, 68, 70}));
    }

    // Vista previa: el mismo PDF, para abrirlo en una pestaña. Sin tipo, los dos juntos
    @Test
    @WithMockUser(username = ME, roles = "SOCIAL_WORKER")
    void thePreviewOpensInTheBrowser() throws Exception {
        when(exportReportsPdfUseCase.ofResident(PATIENT, null)).thenReturn(
                new PdfFile("informes-martinez-lucia.pdf", new byte[]{37, 80, 68, 70}));

        mockMvc.perform(get("/api/reports/residents/{id}/pdf", PATIENT).param("inline", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "inline; filename=\"informes-martinez-lucia.pdf\""));
    }

    @Test
    void withoutTokenNothingIsVisible() throws Exception {
        mockMvc.perform(get("/api/reports")).andExpect(status().isUnauthorized());
    }
}
