package com.viller.gespatiens.reports.infrastructure;

import com.viller.gespatiens.access.users.domain.Speciality;
import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserRepository;
import com.viller.gespatiens.patients.domain.Dni;
import com.viller.gespatiens.patients.domain.Patient;
import com.viller.gespatiens.patients.domain.PatientDetails;
import com.viller.gespatiens.patients.domain.PatientRepository;
import com.viller.gespatiens.reports.domain.Report;
import com.viller.gespatiens.reports.domain.ReportRepository;
import com.viller.gespatiens.reports.domain.ReportSection;
import com.viller.gespatiens.reports.domain.ReportState;
import com.viller.gespatiens.reports.domain.ReportType;
import com.viller.gespatiens.reports.domain.StaffMember;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Contra PostgreSQL de verdad: la tabla de apartados, el índice de un borrador por tipo y el bloqueo
// optimista no se pueden probar en memoria
@SpringBootTest
class ReportPersistenceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 21);
    private static final Instant NOW = Instant.parse("2026-10-21T10:00:00Z");

    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ReportRepository reportRepository;

    // Un antiguo residente (sin número de plaza) para no chocar con las plazas ocupadas de la BD
    private UUID patient() {
        PatientDetails details = new PatientDetails("Prueba", "Informes", new Dni("99999998T"),
                LocalDate.of(1990, 1, 1), "C/ Test 1", "699999100", null, null, null, null, null);
        return patientRepository.save(Patient.reconstitute(UUID.randomUUID(), details, null, DAY.minusYears(1),
                DAY.minusDays(1))).getId();
    }

    private StaffMember employee(String name, String phone, Speciality speciality) {
        User user = userRepository.save(User.create(name, name.toLowerCase() + ".informes@vientos.es", phone,
                speciality, "hash"));
        return new StaffMember(user.getId(), name, speciality.name(), speciality == Speciality.ADMINISTRATOR);
    }

    private void cleanUp(UUID patient, StaffMember... staff) {
        // Sin @Transactional (el bloqueo optimista necesita transacciones reales): se borra a mano.
        // Borrar al residente se lleva sus informes (ON DELETE CASCADE)
        patientRepository.findById(patient).ifPresent(patientRepository::delete);
        for (StaffMember member : staff) {
            userRepository.findById(member.id()).ifPresent(userRepository::delete);
        }
    }

    @Test
    void sectionsHeaderAndStateAreSavedAndReadBack() {
        UUID patient = patient();
        StaffMember marcos = employee("Marcos", "699999101", Speciality.PSYCHOLOGIST);
        StaffMember rosa = employee("Rosa", "699999102", Speciality.ADMINISTRATOR);
        try {
            Report report = reportRepository.save(Report.forStay(patient, ReportType.FINAL, DAY.minusYears(1), NOW));
            report.editHeader("S-1", "UCA", null, "Alta terapéutica", 0);
            report.recordStayEnd(DAY.minusDays(1));
            report.write(ReportSection.PSYCHO_DIAGNOSIS, "Dual", 0, marcos, false, NOW);
            report.write(ReportSection.PSYCHO_AT_DISCHARGE, "Estable", 0, marcos, false, NOW);
            report = reportRepository.save(report);
            report.write(ReportSection.PSYCHO_AT_DISCHARGE, "", 1, marcos, false, NOW);
            report.close(rosa, NOW);
            reportRepository.save(report);

            Report read = reportRepository.findById(report.getId()).orElseThrow();
            assertEquals(ReportState.CLOSED, read.getState());
            assertEquals("Alta terapéutica", read.getHeader().exitReason());
            assertEquals(DAY.minusDays(1), read.getHeader().exitDate());
            assertEquals(DAY.minusYears(1), read.getStayStart());
            assertEquals("Dual", read.getSection(ReportSection.PSYCHO_DIAGNOSIS).orElseThrow().text());
            assertTrue(read.getSection(ReportSection.PSYCHO_AT_DISCHARGE).isEmpty());
            assertEquals(List.of(report.getId()),
                    reportRepository.findByPatient(patient).stream().map(Report::getId).toList());
        } finally {
            cleanUp(patient, marcos, rosa);
        }
    }

    // Dos peticiones que cargan el mismo informe y guardan a la vez: la segunda no pisa a la primera
    @Test
    void aStaleCopyCannotOverwriteANewerSave() {
        UUID patient = patient();
        StaffMember marcos = employee("Marcos", "699999101", Speciality.PSYCHOLOGIST);
        StaffMember pablo = employee("Pablo", "699999103", Speciality.SOCIAL_EDUCATOR);
        try {
            UUID id = reportRepository.save(Report.forStay(patient, ReportType.FINAL, DAY, NOW)).getId();
            Report mine = reportRepository.findById(id).orElseThrow();
            Report theirs = reportRepository.findById(id).orElseThrow();

            theirs.write(ReportSection.ADAPTATION_HABITS, "Bien", 0, pablo, false, NOW);
            reportRepository.save(theirs);
            mine.write(ReportSection.PSYCHO_DIAGNOSIS, "Dual", 0, marcos, false, NOW);

            assertThrows(OptimisticLockingFailureException.class, () -> reportRepository.save(mine));
        } finally {
            cleanUp(patient, marcos, pablo);
        }
    }

    // Si dos pantallas crean a la vez el informe de una estancia, solo queda uno
    @Test
    void thereIsOneReportOfEachTypePerStay() {
        UUID patient = patient();
        try {
            reportRepository.addIfMissing(Report.forStay(patient, ReportType.FINAL, DAY, NOW));
            reportRepository.addIfMissing(Report.forStay(patient, ReportType.FINAL, DAY, NOW));
            reportRepository.addIfMissing(Report.forStay(patient, ReportType.FINAL, DAY.plusDays(30), NOW));

            assertEquals(2, reportRepository.findByPatient(patient).size());
            assertThrows(DataIntegrityViolationException.class, () -> reportRepository.save(
                    Report.forStay(patient, ReportType.FINAL, DAY, NOW)));
        } finally {
            cleanUp(patient);
        }
    }
}
