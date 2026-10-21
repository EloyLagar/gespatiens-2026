package com.viller.gespatiens.audit.application;

import com.viller.gespatiens.audit.domain.AuditLog;

import java.time.Clock;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;

// El registro se conserva 2 años (decisión del centro): lo anterior se borra. Lo lanza AuditPurgeJob cada noche
public class PurgeAuditLogUseCase {

    public static final Period RETENTION = Period.ofYears(2);
    private static final ZoneId CENTER_ZONE = ZoneId.of("Europe/Madrid");

    private final AuditLog auditLog;
    private final Clock clock;

    public PurgeAuditLogUseCase(AuditLog auditLog, Clock clock) {
        this.auditLog = auditLog;
        this.clock = clock;
    }

    // Devuelve cuántas líneas se han borrado
    public long execute() {
        Instant limit = clock.instant().atZone(CENTER_ZONE).minus(RETENTION).toInstant();
        return auditLog.deleteBefore(limit);
    }
}
