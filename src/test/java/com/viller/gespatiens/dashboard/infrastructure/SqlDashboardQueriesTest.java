package com.viller.gespatiens.dashboard.infrastructure;

import com.viller.gespatiens.access.users.domain.Speciality;
import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserRepository;
import com.viller.gespatiens.dashboard.application.DashboardQueries;
import com.viller.gespatiens.dashboard.application.DashboardQueries.ShiftCount;
import com.viller.gespatiens.diary.domain.DayPart;
import com.viller.gespatiens.diary.domain.ShiftLog;
import com.viller.gespatiens.diary.domain.ShiftLogRepository;
import com.viller.gespatiens.patients.domain.Dni;
import com.viller.gespatiens.patients.domain.Patient;
import com.viller.gespatiens.patients.domain.PatientDetails;
import com.viller.gespatiens.patients.domain.PatientRepository;
import com.viller.gespatiens.patients.domain.Referral;
import com.viller.gespatiens.patients.domain.ReferralRepository;
import com.viller.gespatiens.reports.domain.Report;
import com.viller.gespatiens.reports.domain.ReportRepository;
import com.viller.gespatiens.reports.domain.ReportType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Contra PostgreSQL de verdad: las consultas del panel son SQL escrito a mano (FILTER, date_trunc, zonas
// horarias) y solo se pueden comprobar ejecutándolas. @Transactional deshace todo al acabar
@SpringBootTest
@Transactional
class SqlDashboardQueriesTest {

    // Un año lejano, para no mezclarse con los turnos que haya de verdad en la base de datos
    private static final LocalDate DAY = LocalDate.of(2099, 3, 10);

    @Autowired
    private DashboardQueries queries;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ShiftLogRepository shiftLogRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private ReferralRepository referralRepository;
    @Autowired
    private ReportRepository reportRepository;
    @PersistenceContext
    private EntityManager entityManager;

    private UUID employee(String name, String phone) {
        return userRepository.save(User.create(name, name.toLowerCase() + ".panel@vientos.es", phone,
                Speciality.SOCIAL_EDUCATOR, "hash")).getId();
    }

    @Test
    void shiftsAreCountedPerEmployeeAndDayPart() {
        UUID pablo = employee("Pablo", "699999301");
        UUID ana = employee("Ana", "699999302");
        Instant now = Instant.parse("2099-03-10T10:00:00Z");
        shiftLogRepository.save(ShiftLog.record(DAY, DayPart.MORNING, Set.of(pablo, ana), null, pablo, now));
        shiftLogRepository.save(ShiftLog.record(DAY, DayPart.NIGHT, Set.of(pablo), null, pablo, now));
        shiftLogRepository.save(ShiftLog.record(DAY.plusDays(1), DayPart.MORNING, Set.of(pablo), null, pablo, now));
        // Otro mes: no cuenta
        shiftLogRepository.save(ShiftLog.record(DAY.plusMonths(1), DayPart.MORNING, Set.of(pablo), null, pablo, now));

        var counts = queries.shiftsByEmployee(DAY.withDayOfMonth(1), DAY.withDayOfMonth(31));

        ShiftCount pabloCount = counts.stream().filter(c -> c.employeeId().equals(pablo)).findFirst().orElseThrow();
        ShiftCount anaCount = counts.stream().filter(c -> c.employeeId().equals(ana)).findFirst().orElseThrow();
        assertEquals(2, pabloCount.morning());
        assertEquals(1, pabloCount.night());
        assertEquals(3, pabloCount.total());
        assertEquals(1, anaCount.total());
        assertEquals("SOCIAL_EDUCATOR", anaCount.speciality());
    }

    @Test
    void theOtherFiguresRun() {
        assertTrue(queries.activeResidents() >= 0);
        assertTrue(queries.activeResidentsWithoutBothTutors() <= queries.activeResidents());
        assertTrue(queries.reportsInProgress() >= 0);
        assertTrue(queries.openMidStayReports().size() <= queries.activeResidents());
        assertTrue(queries.courtMeasures().size() <= queries.activeResidents());
        assertEquals(0, queries.interventionsBetween(DAY, DAY));
        assertTrue(queries.noticesByWeek(DAY, DAY.plusDays(6)).isEmpty());
    }

    // Los vencimientos leen fechas de la base de datos. Llegan como LocalDate: convertirlas a java.sql.Date
    // tumbaba el panel en cuanto había un informe con fecha prevista (octubre de 2026)
    @Test
    void deadlinesReadTheirDates() {
        Instant now = Instant.parse("2026-07-01T10:00:00Z");
        LocalDate entry = LocalDate.of(2026, 7, 1);
        // Una plaza libre: la base de datos puede tener ya residentes en el centro
        int place = ((Number) entityManager.createNativeQuery("""
                SELECT MIN(n) FROM generate_series(1, 32) n
                WHERE n NOT IN (SELECT number FROM patients WHERE number IS NOT NULL)
                """).getSingleResult()).intValue();
        UUID patient = patientRepository.save(Patient.admit(new PatientDetails("Prueba", "Vencimientos Panel",
                new Dni("99999998T"), LocalDate.of(1980, 1, 1), "C/ Test 2", "699999303", null, null, null, null,
                null), place, entry)).getId();
        Report report = Report.forStay(patient, ReportType.MID_STAY, entry, now);
        reportRepository.save(report);
        Referral referral = Referral.none(patient, now);
        referral.update("UCA de Torrent", null, null, true, "Juzgado de lo Penal 3", LocalDate.of(2026, 11, 2), null, now);
        referralRepository.save(referral);

        var midStay = queries.openMidStayReports().stream().filter(r -> r.reportId().equals(report.getId())).findFirst().orElseThrow();
        var measure = queries.courtMeasures().stream().filter(m -> m.resident().id().equals(patient)).findFirst().orElseThrow();
        assertEquals(LocalDate.of(2026, 10, 1), midStay.dueDate());
        assertEquals(LocalDate.of(2026, 11, 2), measure.endDate());
    }
}
