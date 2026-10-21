package com.viller.gespatiens.audit.infrastructure.config;

import com.viller.gespatiens.audit.application.AuditDirectory;
import com.viller.gespatiens.audit.application.PurgeAuditLogUseCase;
import com.viller.gespatiens.audit.application.RecordAuditEntryUseCase;
import com.viller.gespatiens.audit.application.SearchAuditLogUseCase;
import com.viller.gespatiens.audit.domain.AuditLog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

// Los casos de uso no llevan anotaciones de Spring: se registran aquí. EnableScheduling: el borrado nocturno
@Configuration
@EnableScheduling
public class AuditBeanConfiguration {

    @Bean
    public RecordAuditEntryUseCase recordAuditEntryUseCase(AuditLog auditLog, Clock clock) {
        return new RecordAuditEntryUseCase(auditLog, clock);
    }

    @Bean
    public PurgeAuditLogUseCase purgeAuditLogUseCase(AuditLog auditLog, Clock clock) {
        return new PurgeAuditLogUseCase(auditLog, clock);
    }

    @Bean
    public SearchAuditLogUseCase searchAuditLogUseCase(AuditLog auditLog, AuditDirectory directory) {
        return new SearchAuditLogUseCase(auditLog, directory);
    }
}
