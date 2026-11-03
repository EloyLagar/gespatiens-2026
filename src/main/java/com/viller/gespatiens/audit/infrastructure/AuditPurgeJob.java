package com.viller.gespatiens.audit.infrastructure;

import com.viller.gespatiens.audit.application.PurgeAccessLogUseCase;
import com.viller.gespatiens.audit.application.PurgeAuditLogUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Borra de los dos registros (cambios y accesos) lo que ha pasado el plazo de conservación: cada noche y al arrancar, por si el servidor estuvo
// apagado a esa hora
@Component
public class AuditPurgeJob {

    private static final Logger log = LoggerFactory.getLogger(AuditPurgeJob.class);

    private final PurgeAuditLogUseCase purgeAuditLog;
    private final PurgeAccessLogUseCase purgeAccessLog;

    public AuditPurgeJob(PurgeAuditLogUseCase purgeAuditLog, PurgeAccessLogUseCase purgeAccessLog) {
        this.purgeAuditLog = purgeAuditLog;
        this.purgeAccessLog = purgeAccessLog;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "0 30 3 * * *", zone = "Europe/Madrid")
    public void purge() {
        long deleted = purgeAuditLog.execute();
        if (deleted > 0) {
            log.info("Registro de cambios: borradas {} líneas con más de {}", deleted, PurgeAuditLogUseCase.RETENTION);
        }
        long accesses = purgeAccessLog.execute();
        if (accesses > 0) {
            log.info("Registro de accesos: borradas {} líneas con más de {}", accesses, PurgeAuditLogUseCase.RETENTION);
        }
    }
}
