package com.viller.gespatiens.audit.infrastructure.web;

import com.viller.gespatiens.audit.application.AuditDirectory;
import com.viller.gespatiens.audit.application.RecordAuditEntryUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

// Anota en el registro las peticiones de AuditRules que han ido bien (2xx). El residente se averigua antes
// de atender la petición: después de borrar una intervención ya no se podría saber de quién era
public class AuditInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuditInterceptor.class);
    private static final String MATCH = AuditInterceptor.class.getName() + ".match";
    private static final String PATIENT = AuditInterceptor.class.getName() + ".patient";
    // Lo deja CreatedIdAdvice: el id de lo que acaba de crear la petición
    static final String CREATED_ID = AuditInterceptor.class.getName() + ".createdId";

    private final RecordAuditEntryUseCase recordAuditEntry;
    private final AuditDirectory directory;

    public AuditInterceptor(RecordAuditEntryUseCase recordAuditEntry, AuditDirectory directory) {
        this.recordAuditEntry = recordAuditEntry;
        this.directory = directory;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        AuditRules.match(request.getMethod(), request.getRequestURI()).ifPresent(match -> {
            request.setAttribute(MATCH, match);
            try {
                resolvePatient(match).ifPresent(patient -> request.setAttribute(PATIENT, patient));
            } catch (RuntimeException e) {
                log.warn("No se ha podido saber de qué residente es {} {}", request.getMethod(), request.getRequestURI(), e);
            }
        });
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        if (!(request.getAttribute(MATCH) instanceof AuditRules.Match match)) {
            return;
        }
        Principal principal = request.getUserPrincipal();
        if (ex != null || response.getStatus() >= 300 || principal == null) {
            return;
        }
        // Que el registro falle no puede estropear una petición que ya ha ido bien
        try {
            UUID patient = match.rule().source() == AuditRules.Source.CREATED
                    ? (UUID) request.getAttribute(CREATED_ID)
                    : (UUID) request.getAttribute(PATIENT);
            recordAuditEntry.execute(UUID.fromString(principal.getName()), match.rule().action(), patient,
                    resourceOf(match));
        } catch (RuntimeException e) {
            log.warn("No se ha podido anotar {} {} en el registro de accesos", request.getMethod(),
                    request.getRequestURI(), e);
        }
    }

    private Optional<UUID> resolvePatient(AuditRules.Match match) {
        return switch (match.rule().source()) {
            case PATH -> uuid(match.variables().get("patient"));
            case REPORT -> uuid(match.variables().get("id")).flatMap(directory::residentOfReport);
            case INTERVENTION -> uuid(match.variables().get("id")).flatMap(directory::residentOfIntervention);
            case CREATED -> Optional.empty();
        };
    }

    // El informe o la intervención concretos
    private static UUID resourceOf(AuditRules.Match match) {
        return switch (match.rule().source()) {
            case REPORT, INTERVENTION -> uuid(match.variables().get("id")).orElse(null);
            default -> null;
        };
    }

    // Una ruta con un id mal formado acaba en 400 y no se anota; aquí basta con no fallar
    private static Optional<UUID> uuid(String value) {
        try {
            return Optional.ofNullable(value).map(UUID::fromString);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
