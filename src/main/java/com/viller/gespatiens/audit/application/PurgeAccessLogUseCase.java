package com.viller.gespatiens.audit.application;

import com.viller.gespatiens.audit.domain.AccessLog;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

// El registro de accesos se conserva lo mismo que el de cambios (PurgeAuditLogUseCase.RETENTION): lo anterior se
// borra. Lo lanza AuditPurgeJob cada noche
public class PurgeAccessLogUseCase {

    private static final ZoneId CENTER_ZONE = ZoneId.of("Europe/Madrid");

    private final AccessLog accessLog;
    private final Clock clock;

    public PurgeAccessLogUseCase(AccessLog accessLog, Clock clock) {
        this.accessLog = accessLog;
        this.clock = clock;
    }

    // Devuelve cuántas líneas se han borrado
    public long execute() {
        Instant limit = clock.instant().atZone(CENTER_ZONE).minus(PurgeAuditLogUseCase.RETENTION).toInstant();
        return accessLog.deleteBefore(limit);
    }
}
