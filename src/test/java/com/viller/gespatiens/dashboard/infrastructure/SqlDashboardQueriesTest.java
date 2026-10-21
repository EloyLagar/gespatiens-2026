package com.viller.gespatiens.dashboard.infrastructure;

import com.viller.gespatiens.access.users.domain.Speciality;
import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserRepository;
import com.viller.gespatiens.dashboard.application.DashboardQueries;
import com.viller.gespatiens.dashboard.application.DashboardQueries.ShiftCount;
import com.viller.gespatiens.diary.domain.DayPart;
import com.viller.gespatiens.diary.domain.ShiftLog;
import com.viller.gespatiens.diary.domain.ShiftLogRepository;
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
        assertEquals(0, queries.interventionsBetween(DAY, DAY));
        assertTrue(queries.noticesByWeek(DAY, DAY.plusDays(6)).isEmpty());
    }
}
