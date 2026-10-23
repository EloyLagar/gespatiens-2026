package com.viller.gespatiens.reports.infrastructure.pdf;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.viller.gespatiens.reports.application.PrintableReport;
import com.viller.gespatiens.reports.application.PrintableReport.Signer;
import com.viller.gespatiens.reports.application.ReportPdfRenderer;
import com.viller.gespatiens.reports.domain.Report;
import com.viller.gespatiens.reports.domain.ReportArea;
import com.viller.gespatiens.reports.domain.ReportHeader;
import com.viller.gespatiens.reports.domain.ReportSection;
import com.viller.gespatiens.reports.domain.ReportType;
import com.viller.gespatiens.reports.domain.Resident;
import com.viller.gespatiens.reports.domain.SectionEntry;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Los informes con el formato de los modelos del centro (memoria, figuras 9 y 10): título subrayado,
// recuadro con los datos del residente, áreas con el encabezado en granate y la lista de apartados,
// y al final quién lo ha elaborado y el hueco para la firma de la Dirección
public class OpenPdfReportRenderer implements ReportPdfRenderer {

    private static final Color INK = new Color(0x16, 0x16, 0x16);
    private static final Color MUTED = new Color(0x6f, 0x6f, 0x6f);
    // El granate de los encabezados de área de los modelos del centro
    private static final Color AREA = new Color(0x9b, 0x1c, 0x1c);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    // Las fechas de cierre se guardan en UTC; en el papel van en la hora del centro
    private static final ZoneId CENTRE = ZoneId.of("Europe/Madrid");

    private static final Font TITLE = font(15, Font.BOLD | Font.UNDERLINE, INK);
    private static final Font AREA_TITLE = font(12, Font.BOLD | Font.UNDERLINE, AREA);
    private static final Font LABEL = font(10, Font.BOLD, INK);
    private static final Font TEXT = font(10, Font.NORMAL, INK);
    private static final Font EMPTY = font(10, Font.ITALIC, MUTED);
    private static final Font BOX_LABEL = font(8, Font.BOLD, INK);
    private static final Font BOX_VALUE = font(8, Font.NORMAL, INK);
    private static final Font SMALL = font(8, Font.NORMAL, MUTED);

    // Los logos del centro, leídos una sola vez
    private static final byte[] SISAN = logo("logo_SISAN_transparente.png");
    private static final byte[] SIGED = logo("logo_SIGED_transparente.png");

    private static final Map<String, String> SPECIALITIES = Map.of(
            "ADMINISTRATOR", "Administración",
            "SOCIAL_EDUCATOR", "Educador/a social",
            "SOCIAL_WORKER", "Trabajador/a social",
            "PSYCHOLOGIST", "Psicólogo/a",
            "MEDICAL_TEAM", "Equipo médico");

    @Override
    public byte[] render(List<PrintableReport> reports) {
        if (reports.isEmpty()) {
            throw new IllegalArgumentException("No hay informes que imprimir");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 56, 56, 56, 64);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            Footer footer = new Footer();
            writer.setPageEvent(footer);
            document.addTitle(reports.getFirst().report().getType().title());
            document.addCreator("gespatiens");
            boolean first = true;
            for (PrintableReport printable : reports) {
                // El pie es de cada informe: se cambia antes de empezar su primera página
                footer.current = printable;
                if (first) {
                    document.open();
                    first = false;
                } else {
                    document.newPage();
                }
                write(document, printable);
            }
            document.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("No se ha podido generar el PDF del informe", e);
        }
        return out.toByteArray();
    }

    private void write(Document document, PrintableReport printable) throws DocumentException {
        Report report = printable.report();
        ReportType type = report.getType();

        document.add(logos());
        Paragraph title = new Paragraph(type.title().toUpperCase(), TITLE);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(14);
        document.add(title);
        if (report.isDraft()) {
            Paragraph draft = new Paragraph("Borrador: el informe todavía está en elaboración", SMALL);
            draft.setAlignment(Element.ALIGN_CENTER);
            draft.setSpacingAfter(10);
            document.add(draft);
        }
        document.add(headerBox(printable.resident(), report));

        ReportArea current = null;
        boolean adaptationOpen = false;
        for (ReportSection section : ReportSection.of(type)) {
            if (section.area() != current) {
                current = section.area();
                adaptationOpen = false;
                Paragraph area = new Paragraph(current.title(type), AREA_TITLE);
                area.setSpacingBefore(18);
                area.setSpacingAfter(6);
                area.setKeepTogether(true);
                document.add(area);
            }
            if (section.adaptation() && !adaptationOpen) {
                adaptationOpen = true;
                document.add(item(ReportSection.ADAPTATION_HEADING, null, 0, "•"));
            }
            Optional<SectionEntry> entry = report.getSection(section);
            document.add(item(section.label(), entry.map(SectionEntry::text).orElse(null),
                    // Segundo nivel con una raya: el círculo blanco no está en CP1252 y una "o" parecía una letra
                    section.adaptation() ? 18 : 0, section.adaptation() ? "–" : "•"));
        }

        document.add(signatures(printable));
    }

    // La cabecera del centro: SISAN a la izquierda y SIGED a la derecha, como en sus modelos. Se usa la versión
    // transparente de cada logo, que queda bien sobre cualquier papel
    private static PdfPTable logos() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingAfter(18);
        table.addCell(logoCell(SISAN, 40, Element.ALIGN_LEFT));
        table.addCell(logoCell(SIGED, 40, Element.ALIGN_RIGHT));
        return table;
    }

    private static PdfPCell logoCell(byte[] png, float height, int alignment) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        try {
            Image image = Image.getInstance(png);
            image.scaleToFit(220, height);
            image.setAlignment(alignment);
            cell.addElement(image);
        } catch (IOException | RuntimeException unreadable) {
            // Sin logo el informe sigue siendo válido: no se rompe el PDF por una imagen
        }
        return cell;
    }

    private static byte[] logo(String name) {
        try (InputStream in = OpenPdfReportRenderer.class.getResourceAsStream("/reports/logos/" + name)) {
            if (in == null) {
                throw new IllegalStateException("Falta el logo " + name + " en src/main/resources/reports/logos");
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("No se ha podido leer el logo " + name, e);
        }
    }

    // El recuadro con los datos del residente, como en los modelos: etiqueta en negrita y valor
    private PdfPTable headerBox(Resident resident, Report report) {
        ReportHeader h = report.getHeader();
        PdfPTable inner = new PdfPTable(new float[]{2.2f, 3.6f, 1.2f, 2.4f});
        inner.setWidthPercentage(100);
        row(inner, "Apellidos y nombre:", resident.surname() + ", " + resident.name(), null, null);
        row(inner, "Fecha de nacimiento:", date(resident.birthDate()), "DNI:", resident.dni());
        row(inner, "Centro de referencia:", h.referenceCenter(), null, null);
        if (report.getType() == ReportType.MID_STAY) {
            row(inner, "Nº de solicitud:", h.requestNumber(), "N.I.P.:", h.nip());
            row(inner, "Fecha de ingreso:", date(h.entryDate()), null, null);
        } else {
            row(inner, "Nº de solicitud:", h.requestNumber(), "S.I.P.:", resident.sip());
            row(inner, "Fecha de entrada:", date(h.entryDate()), null, null);
            row(inner, "Fecha de salida:", date(h.exitDate()), null, null);
            row(inner, "Motivo de salida:", h.exitReason(), null, null);
        }
        PdfPCell box = new PdfPCell(inner);
        box.setBorder(Rectangle.BOX);
        box.setBorderColor(INK);
        box.setPadding(6);
        PdfPTable outer = new PdfPTable(1);
        outer.setWidthPercentage(62);
        outer.setHorizontalAlignment(Element.ALIGN_LEFT);
        outer.addCell(box);
        outer.setSpacingAfter(6);
        return outer;
    }

    private static void row(PdfPTable table, String label, String value, String label2, String value2) {
        table.addCell(cell(label, BOX_LABEL));
        table.addCell(cell(value, BOX_VALUE));
        table.addCell(cell(label2, BOX_LABEL));
        table.addCell(cell(value2, BOX_VALUE));
    }

    private static PdfPCell cell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(3);
        return cell;
    }

    // Un apartado: viñeta y etiqueta en negrita, y debajo lo escrito. Uno vacío lo dice en gris
    private static Paragraph item(String label, String text, float indent, String bullet) {
        Paragraph paragraph = new Paragraph();
        paragraph.setIndentationLeft(indent + 12);
        paragraph.setFirstLineIndent(-12);
        paragraph.setSpacingBefore(4);
        paragraph.add(new Chunk(bullet + "  ", LABEL));
        paragraph.add(new Chunk(label, LABEL));
        if (text != null) {
            paragraph.add(Chunk.NEWLINE);
            paragraph.add(new Chunk(text, TEXT));
        } else if (!label.endsWith(":")) {
            paragraph.add(Chunk.NEWLINE);
            paragraph.add(new Chunk("Sin redactar", EMPTY));
        }
        paragraph.setLeading(14);
        return paragraph;
    }

    // "El presente informe ha sido elaborado por los siguientes profesionales: nombre, puesto profesional y
    // firma" y "Fecha y firma de la Dirección del recurso"
    private PdfPTable signatures(PrintableReport printable) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingBefore(24);
        table.setKeepTogether(true);
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.addElement(new Paragraph("El presente informe ha sido elaborado por los siguientes profesionales:",
                TEXT));
        if (printable.authors().isEmpty()) {
            cell.addElement(new Paragraph("Nombre, puesto profesional y firma", EMPTY));
        }
        if (!printable.authors().isEmpty()) {
            cell.addElement(signers(printable.authors()));
        }
        Paragraph direction = new Paragraph("Fecha y firma de la Dirección del recurso:", TEXT);
        direction.setSpacingBefore(24);
        cell.addElement(direction);
        String closed = printable.report().getClosedAt()
                .map(instant -> DATE.format(instant.atZone(CENTRE).toLocalDate()))
                .orElse("____/____/________");
        Paragraph signature = new Paragraph(closed + "        Firma: ______________________", TEXT);
        signature.setSpacingBefore(10);
        cell.addElement(signature);
        table.addCell(cell);
        return table;
    }

    // Nombre, puesto y firma de cada profesional. Quien no ha subido su firma escaneada deja el hueco para
    // firmar a mano
    private static PdfPTable signers(List<Signer> signers) {
        PdfPTable table = new PdfPTable(new float[]{3f, 2f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(6);
        for (Signer signer : signers) {
            Paragraph who = new Paragraph();
            who.add(new Chunk(signer.member().name(), LABEL));
            who.add(Chunk.NEWLINE);
            who.add(new Chunk(SPECIALITIES.getOrDefault(signer.member().speciality(),
                    signer.member().speciality()), TEXT));
            PdfPCell name = new PdfPCell(who);
            name.setBorder(Rectangle.NO_BORDER);
            name.setVerticalAlignment(Element.ALIGN_MIDDLE);
            name.setPaddingBottom(10);
            table.addCell(name);
            table.addCell(signatureCell(signer));
        }
        return table;
    }

    private static PdfPCell signatureCell(Signer signer) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(MUTED);
        cell.setFixedHeight(48);
        cell.setVerticalAlignment(Element.ALIGN_BOTTOM);
        signer.signature().ifPresent(png -> {
            try {
                Image image = Image.getInstance(png);
                // La firma cabe en el hueco sin deformarse
                image.scaleToFit(150, 42);
                cell.setImage(image);
            } catch (IOException | RuntimeException unreadable) {
                // Una imagen que no se puede leer deja el hueco para firmar a mano en vez de romper el PDF
            }
        });
        return cell;
    }

    private static String date(LocalDate date) {
        return date == null ? "" : DATE.format(date);
    }

    private static Font font(float size, int style, Color color) {
        // CP1252 tiene las tildes y la ñ del castellano con las fuentes estándar del PDF
        return FontFactory.getFont(FontFactory.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED, size, style, color);
    }

    // Pie de cada página: de quién es el informe y el número de página
    private static final class Footer extends PdfPageEventHelper {

        private PrintableReport current;

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            if (current == null) {
                return;
            }
            Resident resident = current.resident();
            String text = current.report().getType().title() + " · " + resident.surname() + ", " + resident.name()
                    + " · Página " + writer.getPageNumber();
            PdfContentByte canvas = writer.getDirectContent();
            ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT, new Phrase(text, SMALL),
                    document.right(), document.bottom() - 24, 0);
        }
    }
}
