package com.viller.gespatiens.reports.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportDomainTest {

    private static final Instant NOW = Instant.parse("2026-10-21T10:00:00Z");
    private static final Instant LATER = NOW.plusSeconds(600);
    private static final LocalDate STAY = LocalDate.of(2026, 4, 1);

    private final StaffMember psychologist = new StaffMember(UUID.randomUUID(), "Marcos", "PSYCHOLOGIST", false);
    private final StaffMember otherPsychologist = new StaffMember(UUID.randomUUID(), "Elena", "PSYCHOLOGIST", false);
    private final StaffMember educator = new StaffMember(UUID.randomUUID(), "Pablo", "SOCIAL_EDUCATOR", false);
    private final StaffMember socialWorker = new StaffMember(UUID.randomUUID(), "Nuria", "SOCIAL_WORKER", false);
    private final StaffMember doctor = new StaffMember(UUID.randomUUID(), "Irene", "MEDICAL_TEAM", false);
    private final StaffMember admin = new StaffMember(UUID.randomUUID(), "Rosa", "ADMINISTRATOR", true);

    private Report midStay() {
        return Report.forStay(UUID.randomUUID(), ReportType.MID_STAY, STAY, NOW);
    }

    private Report finalReport() {
        return Report.forStay(UUID.randomUUID(), ReportType.FINAL, STAY, NOW);
    }

    // Los apartados salen de los modelos del centro: cada informe tiene los suyos y en su orden
    @Test
    void eachReportHasTheSectionsOfTheCentreModel() {
        List<ReportSection> midStay = ReportSection.of(ReportType.MID_STAY);
        List<ReportSection> fin = ReportSection.of(ReportType.FINAL);

        assertEquals(ReportSection.SOCIAL_FAMILY_SITUATION, midStay.getFirst());
        assertEquals(ReportSection.PROGRAM_DURATION_FORECAST, midStay.getLast());
        assertEquals(ReportSection.AFTER_DISCHARGE_OBJECTIVES, fin.getLast());
        assertTrue(midStay.contains(ReportSection.SOCIAL_OBJECTIVES));
        assertFalse(fin.contains(ReportSection.SOCIAL_OBJECTIVES));
        assertTrue(fin.contains(ReportSection.HEALTH_AT_DISCHARGE));
        assertEquals(5, midStay.stream().filter(ReportSection::adaptation).count());
        assertNull(ReportArea.FOLLOW_UP.title(ReportType.MID_STAY));
        assertNull(ReportArea.DURATION.title(ReportType.FINAL));
    }

    // Un informe nuevo es de una estancia y trae el centro de referencia por defecto
    @Test
    void aStayReportStartsEmptyWithTheDefaultReferenceCentre() {
        Report report = midStay();

        assertEquals(STAY, report.getStayStart());
        assertEquals(STAY, report.getHeader().entryDate());
        assertEquals(ReportHeader.DEFAULT_REFERENCE_CENTER, report.getHeader().referenceCenter());
        assertTrue(report.isDraft());
        assertTrue(report.getCreatedBy().isEmpty());
        assertEquals(0, report.writtenSections());
    }

    // Memoria final: cada especialidad su área y la administración todas
    @Test
    void eachAreaIsWrittenByItsSpecialityAndAdministrationWritesAll() {
        Report report = midStay();

        report.write(ReportSection.PSYCHO_DIAGNOSIS, "  Diagnóstico dual  ", 0, psychologist, false, NOW);
        report.write(ReportSection.ADAPTATION_HABITS, "Buena adaptación", 0, educator, false, NOW);
        report.write(ReportSection.SOCIAL_DIAGNOSIS, "Sin incidencias", 0, admin, false, NOW);

        assertThrows(AreaNotWritableException.class,
                () -> report.write(ReportSection.HEALTH_AT_ENTRY, "Estable", 0, educator, false, NOW));
        assertThrows(AreaNotWritableException.class,
                () -> report.write(ReportSection.SOCIAL_DIAGNOSIS, "Otro", 1, doctor, false, NOW));
        SectionEntry entry = report.getSection(ReportSection.PSYCHO_DIAGNOSIS).orElseThrow();
        assertEquals("Diagnóstico dual", entry.text());
        assertEquals(psychologist.id(), entry.authorId());
        assertEquals(1, entry.version());
        assertEquals(3, report.writtenSections());
    }

    // La familiar la redacta trabajo social (decisión del centro)
    @Test
    void theFamilyAreaIsWrittenBySocialWork() {
        Report report = midStay();

        report.write(ReportSection.FAMILY_RELATIONSHIP, "Buena", 0, socialWorker, false, NOW);

        assertThrows(AreaNotWritableException.class,
                () -> report.write(ReportSection.REFERENCE_RELATIVES, "Madre", 0, psychologist, false, NOW));
    }

    // La previsión de duración y las orientaciones de seguimiento: los tutores del residente y la administración
    @Test
    void theConclusionsAreWrittenByTheTutorsAndAdministration() {
        Report mid = midStay();
        Report fin = finalReport();

        mid.write(ReportSection.PROGRAM_DURATION_FORECAST, "6 meses", 0, educator, true, NOW);
        fin.write(ReportSection.DISCHARGE_GROUNDS, "Alta terapéutica", 0, psychologist, true, NOW);
        fin.write(ReportSection.AFTER_DISCHARGE_OBJECTIVES, "Seguimiento en UCA", 0, admin, false, NOW);

        assertThrows(AreaNotWritableException.class,
                () -> mid.write(ReportSection.PROGRAM_DURATION_FORECAST, "8 meses", 1, otherPsychologist, false,
                        NOW));
        assertThrows(AreaNotWritableException.class,
                () -> fin.write(ReportSection.DISCHARGE_GROUNDS, "Otro", 1, doctor, false, NOW));
    }

    @Test
    void sectionsOfTheOtherReportAreRejected() {
        Report report = midStay();

        assertThrows(IllegalArgumentException.class,
                () -> report.write(ReportSection.HEALTH_AT_DISCHARGE, "Alta", 0, doctor, false, NOW));
    }

    // RF8: si otra persona guardó el apartado mientras lo editabas, tu guardado no borra lo suyo
    @Test
    void savingOverSomeoneElsesChangeIsAConflict() {
        Report report = midStay();
        report.write(ReportSection.PSYCHO_DIAGNOSIS, "Primera versión", 0, psychologist, false, NOW);
        report.write(ReportSection.PSYCHO_DIAGNOSIS, "Segunda versión", 1, otherPsychologist, false, LATER);

        SectionConflictException conflict = assertThrows(SectionConflictException.class,
                () -> report.write(ReportSection.PSYCHO_DIAGNOSIS, "La mía", 1, psychologist, false, LATER));

        assertEquals(ReportSection.PSYCHO_DIAGNOSIS, conflict.getSection());
        assertEquals("Segunda versión", conflict.getCurrent().orElseThrow().text());
        assertEquals(2, conflict.getCurrent().orElseThrow().version());
        // Con la versión actual delante, ya puede guardar la suya encima
        report.write(ReportSection.PSYCHO_DIAGNOSIS, "La mía", 2, psychologist, false, LATER);
        assertEquals(3, report.getSection(ReportSection.PSYCHO_DIAGNOSIS).orElseThrow().version());
    }

    @Test
    void writingOnAnEmptySectionThatSomeoneFilledIsAConflictToo() {
        Report report = midStay();
        report.write(ReportSection.PSYCHO_DIAGNOSIS, "Suya", 0, otherPsychologist, false, NOW);

        assertThrows(SectionConflictException.class,
                () -> report.write(ReportSection.PSYCHO_DIAGNOSIS, "Mía", 0, psychologist, false, LATER));
    }

    @Test
    void savingWithoutChangesKeepsAuthorAndVersionAndBlankEmptiesTheSection() {
        Report report = midStay();
        report.write(ReportSection.PSYCHO_DIAGNOSIS, "Texto", 0, psychologist, false, NOW);

        report.write(ReportSection.PSYCHO_DIAGNOSIS, "Texto ", 1, otherPsychologist, false, LATER);
        SectionEntry unchanged = report.getSection(ReportSection.PSYCHO_DIAGNOSIS).orElseThrow();
        assertEquals(psychologist.id(), unchanged.authorId());
        assertEquals(1, unchanged.version());

        report.write(ReportSection.PSYCHO_DIAGNOSIS, "   ", 1, psychologist, false, LATER);
        assertTrue(report.getSection(ReportSection.PSYCHO_DIAGNOSIS).isEmpty());
        assertEquals(0, report.writtenSections());
    }

    @Test
    void sectionsHaveAMaximumLength() {
        Report report = midStay();

        assertThrows(IllegalArgumentException.class, () -> report.write(ReportSection.PSYCHO_DIAGNOSIS,
                "x".repeat(SectionEntry.MAX_LENGTH + 1), 0, psychologist, false, NOW));
    }

    // Un informe cerrado no se modifica por error hasta que la administración lo reabra
    @Test
    void onlyAdministrationClosesAndReopensAndAClosedReportCannotChange() {
        Report report = midStay();

        assertThrows(ReportNotManageableException.class, () -> report.close(psychologist, NOW));
        report.close(admin, NOW);

        assertEquals(ReportState.CLOSED, report.getState());
        assertEquals(admin.id(), report.getClosedBy().orElseThrow());
        assertThrows(ReportClosedException.class,
                () -> report.write(ReportSection.PSYCHO_DIAGNOSIS, "Tarde", 0, psychologist, false, LATER));
        assertThrows(ReportClosedException.class, () -> report.editHeader("S-1", null, null, null, null, 0));
        assertThrows(ReportNotManageableException.class, () -> report.reopen(educator));

        report.reopen(admin);
        assertTrue(report.isDraft());
        assertTrue(report.getClosedAt().isEmpty());
        report.write(ReportSection.PSYCHO_DIAGNOSIS, "Corregido", 0, psychologist, false, LATER);
    }

    // A mano solo se escribe lo que no está en la ficha. Las fechas de la estancia no se tocan
    @Test
    void theHeaderOnlyChangesWhatIsWrittenByHandAndDetectsConflicts() {
        Report report = midStay();

        report.editHeader("S-12", "UCA Torrent", "N-1", "Alta", null, 0);

        ReportHeader header = report.getHeader();
        assertEquals("UCA Torrent", header.referenceCenter());
        assertEquals("N-1", header.nip());
        assertNull(header.exitReason());
        assertEquals(STAY, header.entryDate());
        assertEquals(1, report.getHeaderVersion());
        assertThrows(HeaderConflictException.class, () -> report.editHeader("S-13", null, null, null, null, 0));
    }

    // Decisión del centro: el de media estancia toca a mitad del programa; por defecto a los 3 meses y se puede
    // cambiar. El final no tiene fecha prevista
    @Test
    void theMidStayReportIsDueHalfwayAndTheDateCanBeChanged() {
        Report report = midStay();
        assertEquals(STAY.plusMonths(3), report.getHeader().dueDate());
        assertNull(finalReport().getHeader().dueDate());

        report.editHeader(null, null, null, null, STAY.plusMonths(4), 0);
        assertEquals(STAY.plusMonths(4), report.getHeader().dueDate());

        // Sin fecha vuelve la de por defecto; antes de la entrada no puede tocar
        report.editHeader(null, null, null, null, null, 1);
        assertEquals(STAY.plusMonths(3), report.getHeader().dueDate());
        assertThrows(IllegalArgumentException.class,
                () -> report.editHeader(null, null, null, null, STAY.minusDays(1), 2));

        Report finalOne = finalReport();
        finalOne.editHeader(null, null, null, null, STAY.plusMonths(4), 0);
        assertNull(finalOne.getHeader().dueDate());
    }

    // Al dar de baja al residente, la salida pasa a la cabecera, aunque el informe ya esté cerrado
    @Test
    void theStayEndIsRecordedFromTheRecord() {
        Report report = finalReport();
        report.close(admin, NOW);

        assertTrue(report.recordStayEnd(LocalDate.of(2026, 10, 1)));
        assertFalse(report.recordStayEnd(LocalDate.of(2026, 10, 1)));

        assertEquals(LocalDate.of(2026, 10, 1), report.getHeader().exitDate());
        assertEquals(0, report.getHeaderVersion());
    }

    // "El presente informe ha sido elaborado por los siguientes profesionales", en el orden de los apartados
    @Test
    void authorsFollowTheOrderOfTheSections() {
        Report report = midStay();
        report.write(ReportSection.ADAPTATION_HABITS, "Bien", 0, educator, false, NOW);
        report.write(ReportSection.PSYCHO_DIAGNOSIS, "Dual", 0, psychologist, false, LATER);
        report.write(ReportSection.PSYCHO_OBJECTIVES, "Abstinencia", 0, psychologist, false, LATER);

        assertEquals(List.of(psychologist.id(), educator.id()), List.copyOf(report.authorIds()));
        assertEquals(LATER, report.lastUpdate());
        assertEquals(Set.of(), midStay().authorIds());
    }
}
