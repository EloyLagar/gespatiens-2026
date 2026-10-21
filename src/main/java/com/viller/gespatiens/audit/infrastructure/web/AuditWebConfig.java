package com.viller.gespatiens.audit.infrastructure.web;

import com.viller.gespatiens.audit.application.AuditDirectory;
import com.viller.gespatiens.audit.application.RecordAuditEntryUseCase;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Engancha el registro de cambios a las peticiones de la API. Los tests de la capa web (@WebMvcTest) cargan esta
// configuración pero no los casos de uso: sin ellos, simplemente no se registra nada
@Configuration
public class AuditWebConfig implements WebMvcConfigurer {

    private final ObjectProvider<RecordAuditEntryUseCase> recordAuditEntry;
    private final ObjectProvider<AuditDirectory> directory;

    public AuditWebConfig(ObjectProvider<RecordAuditEntryUseCase> recordAuditEntry,
                          ObjectProvider<AuditDirectory> directory) {
        this.recordAuditEntry = recordAuditEntry;
        this.directory = directory;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        RecordAuditEntryUseCase record = recordAuditEntry.getIfAvailable();
        AuditDirectory dir = directory.getIfAvailable();
        if (record != null && dir != null) {
            registry.addInterceptor(new AuditInterceptor(record, dir)).addPathPatterns("/api/**");
        }
    }
}
