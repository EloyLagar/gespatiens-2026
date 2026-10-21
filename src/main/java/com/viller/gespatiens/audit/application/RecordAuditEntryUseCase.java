package com.viller.gespatiens.audit.application;

import com.viller.gespatiens.audit.domain.AuditAction;
import com.viller.gespatiens.audit.domain.AuditEntry;
import com.viller.gespatiens.audit.domain.AuditLog;

import java.time.Clock;
import java.util.UUID;

// Anota en el registro un cambio de los datos de un residente. Lo llama la capa web después
// de cada petición que ha ido bien (AuditInterceptor)
public class RecordAuditEntryUseCase {

    private final AuditLog auditLog;
    private final Clock clock;

    public RecordAuditEntryUseCase(AuditLog auditLog, Clock clock) {
        this.auditLog = auditLog;
        this.clock = clock;
    }

    public void execute(UUID actorId, AuditAction action, UUID patientId, UUID resourceId) {
        auditLog.append(AuditEntry.record(clock.instant(), actorId, action, patientId, resourceId));
    }
}
