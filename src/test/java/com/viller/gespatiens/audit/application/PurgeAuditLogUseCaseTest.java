package com.viller.gespatiens.audit.application;

import com.viller.gespatiens.audit.domain.AuditEntry;
import com.viller.gespatiens.audit.domain.AuditLog;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PurgeAuditLogUseCaseTest {

    private Instant asked;

    private final AuditLog auditLog = new AuditLog() {
        @Override
        public void append(AuditEntry entry) {
        }

        @Override
        public long deleteBefore(Instant limit) {
            asked = limit;
            return 7;
        }

        @Override
        public Page search(Filter filter, int page, int size) {
            throw new UnsupportedOperationException();
        }
    };

    // Decisión del centro: el registro se conserva 2 años, contados en la hora del centro
    @Test
    void deletesWhatIsOlderThanTwoYears() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"), ZoneOffset.UTC);

        long deleted = new PurgeAuditLogUseCase(auditLog, clock).execute();

        assertEquals(7, deleted);
        assertEquals(Instant.parse("2024-10-21T10:00:00Z"), asked);
    }
}
