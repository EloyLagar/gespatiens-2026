package com.viller.gespatiens.audit.infrastructure.config;

import com.viller.gespatiens.audit.application.AuditDirectory;
import com.viller.gespatiens.audit.application.RecordAuditEntryUseCase;
import com.viller.gespatiens.audit.application.SearchAuditLogUseCase;
import com.viller.gespatiens.audit.domain.AuditLog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

// Los casos de uso no llevan anotaciones de Spring: se registran aquí
@Configuration
public class AuditBeanConfiguration {

    @Bean
    public RecordAuditEntryUseCase recordAuditEntryUseCase(AuditLog auditLog, Clock clock) {
        return new RecordAuditEntryUseCase(auditLog, clock);
    }

    @Bean
    public SearchAuditLogUseCase searchAuditLogUseCase(AuditLog auditLog, AuditDirectory directory) {
        return new SearchAuditLogUseCase(auditLog, directory);
    }
}
