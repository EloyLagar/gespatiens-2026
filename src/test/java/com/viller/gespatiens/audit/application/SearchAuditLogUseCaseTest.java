package com.viller.gespatiens.audit.application;

import com.viller.gespatiens.audit.domain.AuditAction;
import com.viller.gespatiens.audit.domain.AuditEntry;
import com.viller.gespatiens.audit.domain.AuditLog;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SearchAuditLogUseCaseTest {

    private static final UUID ANA = UUID.randomUUID();
    private static final UUID GONE = UUID.randomUUID();
    private static final UUID LUCIA = UUID.randomUUID();

    private AuditLog.Filter asked;

    private final AuditLog auditLog = new AuditLog() {
        @Override
        public void append(AuditEntry entry) {
        }

        @Override
        public Page search(Filter filter, int page, int size) {
            asked = filter;
            return new Page(List.of(
                    AuditEntry.record(Instant.parse("2026-10-21T09:00:00Z"), ANA, AuditAction.VIEW_RECORD, LUCIA, null),
                    AuditEntry.record(Instant.parse("2026-10-20T09:00:00Z"), GONE, AuditAction.EDIT_RECORD, LUCIA, null),
                    AuditEntry.record(Instant.parse("2026-10-19T09:00:00Z"), ANA, AuditAction.VIEW_REPORT, null, null)),
                    page, 1, 3);
        }
    };

    private final AuditDirectory directory = new AuditDirectory() {
        @Override
        public Map<UUID, String> employeeNames(Collection<UUID> ids) {
            return Map.of(ANA, "Ana");
        }

        @Override
        public Map<UUID, Resident> residents(Collection<UUID> ids) {
            return Map.of(LUCIA, new Resident(LUCIA, 5, "Lucía", "Martínez"));
        }

        @Override
        public Optional<UUID> residentOfReport(UUID reportId) {
            return Optional.empty();
        }

        @Override
        public Optional<UUID> residentOfIntervention(UUID interventionId) {
            return Optional.empty();
        }
    };

    private final SearchAuditLogUseCase useCase = new SearchAuditLogUseCase(auditLog, directory);

    @Test
    void linesComeWithNamesAndDeletedEmployeesStayAnonymous() {
        SearchAuditLogUseCase.Result result = useCase.execute(LUCIA, null, null, null, 0);

        assertEquals(3, result.lines().size());
        assertEquals("Ana", result.lines().get(0).actorName());
        assertEquals("Lucía", result.lines().get(0).resident().name());
        assertNull(result.lines().get(1).actorName());
        assertNull(result.lines().get(2).resident());
        assertEquals(LUCIA, asked.patientId());
    }

    // Los días del filtro son días del centro: el 21 de octubre empieza a las 22:00 UTC del 20 (horario de verano)
    @Test
    void datesAreWholeDaysInTheCentre() {
        useCase.execute(null, ANA, LocalDate.of(2026, 10, 21), LocalDate.of(2026, 10, 21), 0);

        assertEquals(Instant.parse("2026-10-20T22:00:00Z"), asked.from());
        assertEquals(Instant.parse("2026-10-21T22:00:00Z"), asked.to());
        assertEquals(ANA, asked.actorId());
    }

    @Test
    void rejectsReversedDatesAndNegativePages() {
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(null, null, LocalDate.of(2026, 10, 21), LocalDate.of(2026, 10, 1), 0));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null, null, null, null, -1));
    }
}
