package com.viller.gespatiens.audit.application;

import com.viller.gespatiens.audit.domain.AccessEntry;
import com.viller.gespatiens.audit.domain.AccessLog;
import com.viller.gespatiens.audit.domain.AccessOutcome;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

// Lo que ve cada empleado al entrar: su acceso anterior y los intentos fallidos con su email desde entonces
class MyLastAccessUseCaseTest {

    private static final UUID ANA = UUID.randomUUID();
    private static final UUID LUIS = UUID.randomUUID();

    private final List<AccessEntry> stored = new ArrayList<>();

    // Un registro en memoria que filtra como el de verdad
    private final AccessLog accessLog = new AccessLog() {
        @Override
        public void append(AccessEntry entry) {
            stored.add(entry);
        }

        @Override
        public long deleteBefore(Instant limit) {
            return 0;
        }

        @Override
        public Page search(Filter filter, int page, int size) {
            List<AccessEntry> found = stored.stream()
                    .filter(e -> filter.userId() == null || filter.userId().equals(e.getUserId()))
                    .filter(e -> filter.outcomes().isEmpty() || filter.outcomes().contains(e.getOutcome()))
                    .filter(e -> filter.from() == null || !e.getOccurredAt().isBefore(filter.from()))
                    .filter(e -> filter.to() == null || e.getOccurredAt().isBefore(filter.to()))
                    .sorted(Comparator.comparing(AccessEntry::getOccurredAt).reversed())
                    .toList();
            return new Page(found.stream().skip((long) page * size).limit(size).toList(), page, 1, found.size());
        }
    };

    private final MyLastAccessUseCase useCase = new MyLastAccessUseCase(accessLog);

    private void at(String when, UUID user, AccessOutcome outcome, String ip) {
        accessLog.append(AccessEntry.record(Instant.parse(when), "x@vientos.es", user, null, outcome, ip, null));
    }

    @Test
    void thePreviousAccessIsTheOneBeforeNow() {
        at("2026-10-19T07:00:00Z", ANA, AccessOutcome.SUCCESS, "10.0.0.1");
        at("2026-10-20T07:02:00Z", ANA, AccessOutcome.SUCCESS, "10.0.0.2");
        // La de ahora mismo
        at("2026-10-21T07:05:00Z", ANA, AccessOutcome.SUCCESS, "10.0.0.3");

        MyLastAccessUseCase.Result result = useCase.execute(ANA);

        assertEquals("10.0.0.2", result.previous().getIpAddress());
        assertEquals(0, result.failedAttemptsSince());
    }

    // Solo cuentan los fallos con su email después de su acceso anterior, no los de antes ni los de otros
    @Test
    void countsOnlyTheFailuresSinceThePreviousAccess() {
        at("2026-10-19T07:00:00Z", ANA, AccessOutcome.FAILURE, null);
        at("2026-10-20T07:02:00Z", ANA, AccessOutcome.SUCCESS, null);
        at("2026-10-20T23:10:00Z", ANA, AccessOutcome.FAILURE, null);
        at("2026-10-20T23:11:00Z", ANA, AccessOutcome.LOCKED, null);
        at("2026-10-20T23:20:00Z", ANA, AccessOutcome.BLOCKED, null);
        at("2026-10-20T23:30:00Z", LUIS, AccessOutcome.FAILURE, null);
        at("2026-10-21T07:05:00Z", ANA, AccessOutcome.SUCCESS, null);

        assertEquals(3, useCase.execute(ANA).failedAttemptsSince());
    }

    @Test
    void theFirstAccessHasNoPrevious() {
        at("2026-10-21T07:00:00Z", ANA, AccessOutcome.FAILURE, null);
        at("2026-10-21T07:05:00Z", ANA, AccessOutcome.SUCCESS, null);

        MyLastAccessUseCase.Result result = useCase.execute(ANA);

        assertNull(result.previous());
        assertEquals(1, result.failedAttemptsSince());
    }
}
