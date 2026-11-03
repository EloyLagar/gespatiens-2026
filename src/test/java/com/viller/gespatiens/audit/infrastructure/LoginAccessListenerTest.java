package com.viller.gespatiens.audit.infrastructure;

import com.viller.gespatiens.access.users.application.LoginEvents.LoginAttempted;
import com.viller.gespatiens.access.users.application.LoginEvents.Outcome;
import com.viller.gespatiens.access.users.application.LoginOrigin;
import com.viller.gespatiens.audit.application.RecordAccessUseCase;
import com.viller.gespatiens.audit.domain.AccessEntry;
import com.viller.gespatiens.audit.domain.AccessLog;
import com.viller.gespatiens.audit.domain.AccessOutcome;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LoginAccessListenerTest {

    private final List<AccessEntry> stored = new ArrayList<>();
    private boolean broken;

    private final AccessLog accessLog = new AccessLog() {
        @Override
        public void append(AccessEntry entry) {
            if (broken) {
                throw new IllegalStateException("La base de datos no responde");
            }
            stored.add(entry);
        }

        @Override
        public long deleteBefore(Instant limit) {
            return 0;
        }

        @Override
        public Page search(Filter filter, int page, int size) {
            throw new UnsupportedOperationException();
        }
    };

    private final LoginAccessListener listener = new LoginAccessListener(new RecordAccessUseCase(accessLog));

    @Test
    void eachLoginAttemptBecomesALine() {
        UUID ana = UUID.randomUUID();
        Instant at = Instant.parse("2026-10-21T09:00:00Z");

        listener.on(new LoginAttempted(at, "ana@vientos.es", ana, Outcome.LOCKED,
                new LoginOrigin("10.0.0.5", "Firefox")));

        AccessEntry entry = stored.get(0);
        assertEquals(at, entry.getOccurredAt());
        assertEquals("ana@vientos.es", entry.getEmail());
        assertEquals(ana, entry.getUserId());
        assertEquals(AccessOutcome.LOCKED, entry.getOutcome());
        assertEquals("10.0.0.5", entry.getIpAddress());
        assertEquals("Firefox", entry.getUserAgent());
    }

    // Que el registro falle no deja a nadie sin entrar
    @Test
    void aBrokenLogDoesNotStopTheLogin() {
        broken = true;

        assertDoesNotThrow(() -> listener.on(new LoginAttempted(Instant.EPOCH, "ana@vientos.es", null,
                Outcome.SUCCESS, LoginOrigin.UNKNOWN)));
    }

    // Los dos módulos tienen su propia lista de resultados: tienen que seguir coincidiendo
    @Test
    void everyLoginOutcomeHasItsAccessOutcome() {
        for (Outcome outcome : Outcome.values()) {
            assertEquals(outcome.name(), AccessOutcome.valueOf(outcome.name()).name());
        }
    }
}
