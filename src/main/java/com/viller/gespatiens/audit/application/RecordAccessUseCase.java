package com.viller.gespatiens.audit.application;

import com.viller.gespatiens.audit.domain.AccessEntry;
import com.viller.gespatiens.audit.domain.AccessLog;
import com.viller.gespatiens.audit.domain.AccessOutcome;

import java.time.Instant;
import java.util.UUID;

// Anota un intento de entrar o un cambio de contraseña. Lo llama LoginAccessListener con cada aviso del login
public class RecordAccessUseCase {

    private final AccessLog accessLog;

    public RecordAccessUseCase(AccessLog accessLog) {
        this.accessLog = accessLog;
    }

    public void execute(Instant occurredAt, String email, UUID userId, UUID actorId, AccessOutcome outcome,
                        String ipAddress, String userAgent) {
        accessLog.append(AccessEntry.record(occurredAt, email, userId, actorId, outcome, ipAddress, userAgent));
    }
}
