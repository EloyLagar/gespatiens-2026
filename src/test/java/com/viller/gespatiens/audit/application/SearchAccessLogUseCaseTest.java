package com.viller.gespatiens.audit.application;

import com.viller.gespatiens.audit.domain.AccessEntry;
import com.viller.gespatiens.audit.domain.AccessLog;
import com.viller.gespatiens.audit.domain.AccessOutcome;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SearchAccessLogUseCaseTest {

    private static final UUID ANA = UUID.randomUUID();
    private static final UUID GONE = UUID.randomUUID();

    private AccessLog.Filter asked;
    private List<AccessEntry> stored = List.of();

    private final AccessLog accessLog = new AccessLog() {
        @Override
        public void append(AccessEntry entry) {
        }

        @Override
        public long deleteBefore(Instant limit) {
            return 0;
        }

        @Override
        public Page search(Filter filter, int page, int size) {
            asked = filter;
            return new Page(stored, page, 1, stored.size());
        }
    };

    private final AuditDirectory directory = new AuditDirectory() {
        @Override
        public Map<UUID, String> employeeNames(Collection<UUID> ids) {
            return ids.contains(ANA) ? Map.of(ANA, "Ana") : Map.of();
        }

        @Override
        public Map<UUID, Resident> residents(Collection<UUID> ids) {
            return Map.of();
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

    private final SearchAccessLogUseCase useCase = new SearchAccessLogUseCase(accessLog, directory);

    // Las fechas son días del centro: el 1 de octubre empieza a las 22:00 UTC del 30 de septiembre (horario de verano)
    @Test
    void datesAreWholeDaysInTheCenterTimeZone() {
        useCase.execute(ANA, Set.of(AccessOutcome.FAILURE), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 1), 0);

        assertEquals(ANA, asked.userId());
        assertEquals(Set.of(AccessOutcome.FAILURE), asked.outcomes());
        assertEquals(Instant.parse("2026-09-30T22:00:00Z"), asked.from());
        assertEquals(Instant.parse("2026-10-01T22:00:00Z"), asked.to());
    }

    @Test
    void withoutFiltersEverythingIsAsked() {
        useCase.execute(null, null, null, null, 0);

        assertNull(asked.userId());
        assertEquals(Set.of(), asked.outcomes());
        assertNull(asked.from());
        assertNull(asked.to());
    }

    // Un email que no es de nadie, o de alguien que ya no está, sale sin nombre
    @Test
    void namesTheEmployeesThatStillExist() {
        Instant at = Clock.fixed(Instant.parse("2026-10-21T09:00:00Z"), ZoneOffset.UTC).instant();
        stored = List.of(
                AccessEntry.record(at, "ana@vientos.es", ANA, null, AccessOutcome.SUCCESS, "10.0.0.5", "Firefox"),
                AccessEntry.record(at, "ex@vientos.es", GONE, null, AccessOutcome.FAILURE, "10.0.0.6", null),
                AccessEntry.record(at, "nadie@vientos.es", null, null, AccessOutcome.FAILURE, null, null));

        SearchAccessLogUseCase.Result result = useCase.execute(null, null, null, null, 0);

        assertEquals("Ana", result.lines().get(0).employeeName());
        assertNull(result.lines().get(1).employeeName());
        assertNull(result.lines().get(2).employeeName());
        assertEquals(3, result.totalEntries());
    }

    // La contraseña provisional: de quién es y quién de la administración la puso
    @Test
    void namesWhoResetThePassword() {
        UUID luis = UUID.randomUUID();
        stored = List.of(AccessEntry.record(Instant.EPOCH, "luis@vientos.es", luis, ANA, AccessOutcome.PASSWORD_RESET,
                null, null));

        SearchAccessLogUseCase.Line line = useCase.execute(null, null, null, null, 0).lines().get(0);

        assertNull(line.employeeName());
        assertEquals("Ana", line.actorName());
    }

    @Test
    void theEndCannotBeBeforeTheStart() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null, null, LocalDate.of(2026, 10, 2),
                LocalDate.of(2026, 10, 1), 0));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null, null, null, null, -1));
    }

    // Quien prueba contraseñas puede mandar un email enorme: se guarda recortado, no se rechaza
    @Test
    void longEmailsAndBrowsersAreCut() {
        AccessEntry entry = AccessEntry.record(Instant.EPOCH, "a".repeat(400), null, null, AccessOutcome.FAILURE, null,
                "b".repeat(400));

        assertEquals(AccessEntry.MAX_EMAIL, entry.getEmail().length());
        assertEquals(AccessEntry.MAX_USER_AGENT, entry.getUserAgent().length());
    }
}
