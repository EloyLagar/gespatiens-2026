package com.viller.gespatiens.reports.infrastructure.pdf;

import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.viller.gespatiens.reports.application.PrintableReport;
import com.viller.gespatiens.reports.application.PrintableReport.Signer;
import com.viller.gespatiens.reports.domain.Report;
import com.viller.gespatiens.reports.domain.ReportSection;
import com.viller.gespatiens.reports.domain.ReportType;
import com.viller.gespatiens.reports.domain.Resident;
import com.viller.gespatiens.reports.domain.StaffMember;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenPdfReportRendererTest {

    private static final Instant NOW = Instant.parse("2026-10-21T10:00:00Z");

    private final OpenPdfReportRenderer renderer = new OpenPdfReportRenderer();
    private final StaffMember marcos = new StaffMember(UUID.randomUUID(), "Marcos Peña", "PSYCHOLOGIST", false);
    private final StaffMember rosa = new StaffMember(UUID.randomUUID(), "Rosa", "ADMINISTRATOR", true);
    private final Resident lucia = new Resident(UUID.randomUUID(), 5, "Lucía", "Martínez Núñez", "12345678Z",
            LocalDate.of(1990, 5, 3), "SIP-1", LocalDate.of(2026, 4, 1), null, true);

    private Report report(ReportType type) {
        Report report = Report.forStay(lucia.id(), type, LocalDate.of(2026, 4, 1), NOW);
        report.editHeader("S-12", "UCA Quart de Poblet", "N-7", null, null, 0);
        return report;
    }

    private String text(byte[] pdf) throws IOException {
        PdfReader reader = new PdfReader(pdf);
        try {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder all = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                all.append(extractor.getTextFromPage(page)).append('\n');
            }
            return all.toString();
        } finally {
            reader.close();
        }
    }

    @Test
    void theReportFollowsTheCentreModelWithAccentsAndAuthors() throws IOException {
        Report report = report(ReportType.MID_STAY);
        report.write(ReportSection.PSYCHO_DIAGNOSIS, "Trastorno por consumo de cocaína", 0, marcos, false, NOW);

        byte[] pdf = renderer.render(List.of(new PrintableReport(report, lucia,
                List.of(new Signer(marcos, Optional.of(signaturePng()))))));

        assertTrue(new String(pdf, 0, 5).startsWith("%PDF"));
        String text = text(pdf);
        assertTrue(text.contains("INFORME DE MEDIA ESTANCIA"), text);
        assertTrue(text.contains("Martínez Núñez, Lucía"), text);
        assertTrue(text.contains("UCA Quart de Poblet"), text);
        assertTrue(text.contains("ÁREA DE INTERVENCIÓN PSICOLÓGICA"), text);
        assertTrue(text.contains("Trastorno por consumo de cocaína"), text);
        assertTrue(text.contains("Marcos Peña"), text);
        assertTrue(text.contains("Borrador"), text);
        assertTrue(text.contains("PREVISIÓN DE LA DURACIÓN"), text);
    }

    // Los apartados "En cuanto a..." van en un segundo nivel con una raya, no con una "o" que parece una letra
    @Test
    void adaptationItemsUseADashAsSecondLevelBullet() throws IOException {
        Report report = report(ReportType.MID_STAY);
        report.write(ReportSection.ADAPTATION_RULES, "Cumple la normativa", 0, rosa, false, NOW);

        String text = text(renderer.render(List.of(new PrintableReport(report, lucia, List.of()))));

        assertTrue(text.contains("–  En cuanto a normatividad"), text);
        assertFalse(text.contains("o  En cuanto a"), text);
    }

    // RF2: varios informes en un único documento, cada uno desde página nueva
    @Test
    void severalReportsGoOneAfterTheOther() throws IOException {
        Report first = report(ReportType.FINAL);
        first.close(rosa, NOW);
        Report second = report(ReportType.FINAL);

        byte[] pdf = renderer.render(List.of(new PrintableReport(first, lucia, List.of()),
                new PrintableReport(second, lucia, List.of())));

        PdfReader reader = new PdfReader(pdf);
        int pages = reader.getNumberOfPages();
        reader.close();
        assertTrue(pages >= 2);
        String text = text(pdf);
        assertEquals(2, text.split("INFORME FINAL DE PROGRAMA", -1).length - 1, text);
        assertTrue(text.contains("ORIENTACIONES DE SEGUIMIENTO"), text);
        assertTrue(text.contains("21/10/2026"), text);
    }

    // La cabecera del centro: los logos SISAN y SIGED arriba de la primera página de cada informe
    @Test
    void eachReportStartsWithTheCentreLogos() throws IOException {
        Report report = report(ReportType.FINAL);

        byte[] pdf = renderer.render(List.of(new PrintableReport(report, lucia, List.of())));

        PdfReader reader = new PdfReader(pdf);
        try {
            PdfDictionary resources = reader.getPageN(1).getAsDict(PdfName.RESOURCES);
            PdfDictionary images = resources.getAsDict(PdfName.XOBJECT);
            // Los dos logos, cada uno con su máscara de transparencia (los PNG transparentes llevan canal alfa)
            assertEquals(4, images.size());
        } finally {
            reader.close();
        }
    }

    // Una firma que no se puede leer deja el hueco para firmar a mano en lugar de romper el PDF
    @Test
    void anUnreadableSignatureDoesNotBreakThePdf() throws IOException {
        Report report = report(ReportType.MID_STAY);
        report.write(ReportSection.PSYCHO_DIAGNOSIS, "Dual", 0, marcos, false, NOW);

        byte[] pdf = renderer.render(List.of(new PrintableReport(report, lucia,
                List.of(new Signer(marcos, Optional.of(new byte[]{1, 2, 3}))))));

        assertTrue(text(pdf).contains("Marcos Peña"));
    }

    // Un PNG de verdad, como el que sube el empleado
    private static byte[] signaturePng() throws IOException {
        BufferedImage image = new BufferedImage(120, 40, BufferedImage.TYPE_INT_ARGB);
        image.getGraphics().drawLine(5, 30, 110, 10);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    @Test
    void nothingToPrintIsAnError() {
        assertThrows(IllegalArgumentException.class, () -> renderer.render(List.of()));
    }
}
