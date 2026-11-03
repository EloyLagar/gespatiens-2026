package com.viller.gespatiens.audit.application;

import com.viller.gespatiens.audit.domain.AccessEntry;
import com.viller.gespatiens.audit.domain.AccessLog;
import com.viller.gespatiens.audit.domain.AccessOutcome;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// ENS op.acc: al entrar, cada empleado ve cuándo y desde dónde fue su acceso anterior, y cuántos intentos fallidos
// ha habido con su email desde entonces. Así es él quien detecta que alguien ha usado o probado su cuenta.
// La entrada más reciente es la de ahora mismo: la anterior es la segunda
public class MyLastAccessUseCase {

    private final AccessLog accessLog;

    public MyLastAccessUseCase(AccessLog accessLog) {
        this.accessLog = accessLog;
    }

    public Result execute(UUID userId) {
        List<AccessEntry> successes = accessLog.search(
                new AccessLog.Filter(userId, Set.of(AccessOutcome.SUCCESS), null, null), 0, 2).entries();
        AccessEntry previous = successes.size() > 1 ? successes.get(1) : null;
        // Desde justo después de la anterior: los fallos de antes ya se le contaron entonces
        Instant since = previous == null ? null : previous.getOccurredAt().plusNanos(1);
        long failed = accessLog.search(
                new AccessLog.Filter(userId, AccessOutcome.FAILED_LOGINS, since, null), 0, 1).totalEntries();
        return new Result(previous, failed);
    }

    // previous null: es su primera entrada (o la primera desde que hay registro)
    public record Result(AccessEntry previous, long failedAttemptsSince) {
    }
}
