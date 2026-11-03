package com.viller.gespatiens.audit.application;

import com.viller.gespatiens.audit.domain.AccessEntry;
import com.viller.gespatiens.audit.domain.AccessLog;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PurgeAccessLogUseCaseTest {

    private Instant asked;

    private final AccessLog accessLog = new AccessLog() {
        @Override
        public void append(AccessEntry entry) {
        }

        @Override
        public long deleteBefore(Instant limit) {
            asked = limit;
            return 3;
        }

        @Override
        public Page search(Filter filter, int page, int size) {
            throw new UnsupportedOperationException();
        }
    };

    // El mismo plazo que el registro de cambios: 2 años
    @Test
    void deletesWhatIsOlderThanTwoYears() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"), ZoneOffset.UTC);

        long deleted = new PurgeAccessLogUseCase(accessLog, clock).execute();

        assertEquals(3, deleted);
        assertEquals(Instant.parse("2024-10-21T10:00:00Z"), asked);
    }
}
