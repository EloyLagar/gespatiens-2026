package com.viller.gespatiens.dashboard.infrastructure;

import com.viller.gespatiens.dashboard.application.DashboardQueries;
import com.viller.gespatiens.dashboard.application.Deadline;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Las cifras del panel con consultas SQL de solo lectura sobre las tablas de los demás módulos. Contar en la
// base de datos evita cargar meses de turnos o avisos en memoria solo para sumarlos
@Component
public class SqlDashboardQueries implements DashboardQueries {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public List<ShiftCount> shiftsByEmployee(LocalDate from, LocalDate to) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT u.id, u.name, u.speciality,
                               COUNT(*) FILTER (WHERE s.day_part = 'MORNING'),
                               COUNT(*) FILTER (WHERE s.day_part = 'AFTERNOON'),
                               COUNT(*) FILTER (WHERE s.day_part = 'NIGHT')
                        FROM shift_staff ss
                        JOIN shift_logs s ON s.id = ss.shift_log_id
                        JOIN users u ON u.id = ss.user_id
                        WHERE s.date BETWEEN :from AND :to
                        GROUP BY u.id, u.name, u.speciality
                        """)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
        return rows.stream()
                .map(r -> new ShiftCount((UUID) r[0], (String) r[1], (String) r[2], count(r[3]), count(r[4]),
                        count(r[5])))
                .toList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<LocalDate, Integer> noticesByWeek(LocalDate fromMonday, LocalDate toSunday) {
        // Las semanas del centro: la fecha de cada aviso en la hora de España, no en UTC
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT CAST(date_trunc('week', created_at AT TIME ZONE 'Europe/Madrid') AS DATE), COUNT(*)
                        FROM notices
                        WHERE CAST(created_at AT TIME ZONE 'Europe/Madrid' AS DATE) BETWEEN :from AND :to
                        GROUP BY 1
                        """)
                .setParameter("from", fromMonday)
                .setParameter("to", toSunday)
                .getResultList();
        Map<LocalDate, Integer> weeks = new HashMap<>();
        rows.forEach(r -> weeks.put(date(r[0]), count(r[1])));
        return weeks;
    }

    @Override
    public int activeResidents() {
        return count(entityManager.createNativeQuery("SELECT COUNT(*) FROM patients WHERE exit_date IS NULL")
                .getSingleResult());
    }

    @Override
    public int activeResidentsWithoutBothTutors() {
        return count(entityManager.createNativeQuery("""
                        SELECT COUNT(*) FROM patients p
                        WHERE p.exit_date IS NULL
                          AND (SELECT COUNT(*) FROM tutors t WHERE t.patient_id = p.id) < 2
                        """)
                .getSingleResult());
    }

    @Override
    public int reportsInProgress() {
        // Solo los de la estancia en curso de quien sigue en el centro: los de estancias terminadas que nadie
        // cerró inflaban la cifra y no son trabajo pendiente del día a día
        return count(entityManager.createNativeQuery("""
                        SELECT COUNT(*) FROM reports r
                        JOIN patients p ON p.id = r.patient_id
                        WHERE r.state = 'DRAFT'
                          AND p.exit_date IS NULL
                          AND r.stay_start = p.entry_date
                        """)
                .getSingleResult());
    }

    @Override
    public int interventionsBetween(LocalDate from, LocalDate to) {
        return count(entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM interventions WHERE date BETWEEN :from AND :to")
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<OpenMidStayReport> openMidStayReports() {
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT p.id, p.number, p.name, p.surname, r.due_date, r.id
                        FROM reports r
                        JOIN patients p ON p.id = r.patient_id
                        WHERE r.type = 'MID_STAY'
                          AND r.state = 'DRAFT'
                          AND p.exit_date IS NULL
                          AND r.stay_start = p.entry_date
                          AND r.due_date IS NOT NULL
                        """)
                .getResultList();
        return rows.stream()
                .map(r -> new OpenMidStayReport(resident(r), ((Date) r[4]).toLocalDate(), (UUID) r[5]))
                .toList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CourtMeasure> courtMeasures() {
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT p.id, p.number, p.name, p.surname, f.measure_end_date, f.court
                        FROM patient_referrals f
                        JOIN patients p ON p.id = f.patient_id
                        WHERE p.exit_date IS NULL
                          AND f.court_ordered
                          AND f.measure_end_date IS NOT NULL
                        """)
                .getResultList();
        return rows.stream()
                .map(r -> new CourtMeasure(resident(r), ((Date) r[4]).toLocalDate(), (String) r[5]))
                .toList();
    }

    private static Deadline.Resident resident(Object[] row) {
        return new Deadline.Resident((UUID) row[0], row[1] == null ? null : ((Number) row[1]).intValue(),
                (String) row[2], (String) row[3]);
    }

    private static int count(Object value) {
        return ((Number) value).intValue();
    }

    // Según el driver, una fecha llega como java.sql.Date o como LocalDate
    private static LocalDate date(Object value) {
        return value instanceof Date sql ? sql.toLocalDate() : (LocalDate) value;
    }
}
