package com.viller.gespatiens.audit.infrastructure.web;

import com.viller.gespatiens.audit.domain.AuditAction;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.List;
import java.util.Map;
import java.util.Optional;

// Qué peticiones se anotan en el registro y cómo se sabe de qué residente son. Solo las que abren o cambian
// los datos de una persona concreta: los listados no
final class AuditRules {

    // De dónde sale el residente
    enum Source {
        // De la propia ruta (variable "patient")
        PATH,
        // Del id que devuelve la respuesta (el alta crea al residente)
        CREATED,
        // De la tabla de informes, por el id de la ruta
        REPORT,
        // De la tabla de intervenciones, por el id de la ruta
        INTERVENTION
    }

    record Rule(HttpMethod method, PathPattern pattern, AuditAction action, Source source) {
    }

    // La regla que encaja y las variables de la ruta
    record Match(Rule rule, Map<String, String> variables) {
    }

    private static final PathPatternParser PARSER = new PathPatternParser();

    private static final List<Rule> RULES = List.of(
            rule(HttpMethod.GET, "/api/patients/{patient}", AuditAction.VIEW_RECORD, Source.PATH),
            rule(HttpMethod.POST, "/api/patients", AuditAction.ADMIT, Source.CREATED),
            rule(HttpMethod.PUT, "/api/patients/{patient}", AuditAction.EDIT_RECORD, Source.PATH),
            rule(HttpMethod.POST, "/api/patients/{patient}/discharge", AuditAction.DISCHARGE, Source.PATH),
            rule(HttpMethod.POST, "/api/patients/{patient}/readmit", AuditAction.READMIT, Source.PATH),
            rule(HttpMethod.DELETE, "/api/patients/{patient}", AuditAction.DELETE_RECORD, Source.PATH),
            rule(HttpMethod.GET, "/api/patients/{patient}/visitors", AuditAction.VIEW_VISITORS, Source.PATH),
            rule(HttpMethod.POST, "/api/patients/{patient}/visitors", AuditAction.EDIT_VISITORS, Source.PATH),
            rule(HttpMethod.PUT, "/api/patients/{patient}/visitors/{visitor}", AuditAction.EDIT_VISITORS, Source.PATH),
            rule(HttpMethod.DELETE, "/api/patients/{patient}/visitors/{visitor}", AuditAction.EDIT_VISITORS, Source.PATH),
            rule(HttpMethod.GET, "/api/patients/{patient}/referral", AuditAction.VIEW_REFERRAL, Source.PATH),
            rule(HttpMethod.PUT, "/api/patients/{patient}/referral", AuditAction.EDIT_REFERRAL, Source.PATH),
            rule(HttpMethod.GET, "/api/reports/{id}", AuditAction.VIEW_REPORT, Source.REPORT),
            rule(HttpMethod.GET, "/api/reports/{id}/pdf", AuditAction.DOWNLOAD_REPORT, Source.REPORT),
            rule(HttpMethod.GET, "/api/reports/residents/{patient}/pdf", AuditAction.DOWNLOAD_REPORT, Source.PATH),
            rule(HttpMethod.PUT, "/api/reports/{id}/sections/{section}", AuditAction.EDIT_REPORT, Source.REPORT),
            rule(HttpMethod.PUT, "/api/reports/{id}/header", AuditAction.EDIT_REPORT, Source.REPORT),
            rule(HttpMethod.POST, "/api/reports/{id}/close", AuditAction.CLOSE_REPORT, Source.REPORT),
            rule(HttpMethod.POST, "/api/reports/{id}/reopen", AuditAction.REOPEN_REPORT, Source.REPORT),
            rule(HttpMethod.GET, "/api/interventions/residents/{patient}", AuditAction.VIEW_FOLLOW_UP, Source.PATH),
            rule(HttpMethod.POST, "/api/interventions/residents/{patient}", AuditAction.EDIT_FOLLOW_UP, Source.PATH),
            rule(HttpMethod.PUT, "/api/interventions/residents/{patient}/tutors", AuditAction.ASSIGN_TUTORS, Source.PATH),
            rule(HttpMethod.PUT, "/api/interventions/{id}", AuditAction.EDIT_FOLLOW_UP, Source.INTERVENTION),
            rule(HttpMethod.DELETE, "/api/interventions/{id}", AuditAction.EDIT_FOLLOW_UP, Source.INTERVENTION));

    private AuditRules() {
    }

    private static Rule rule(HttpMethod method, String pattern, AuditAction action, Source source) {
        return new Rule(method, PARSER.parse(pattern), action, source);
    }

    static Optional<Match> match(String method, String path) {
        PathContainer container = PathContainer.parsePath(path);
        for (Rule rule : RULES) {
            if (!rule.method().matches(method)) {
                continue;
            }
            PathPattern.PathMatchInfo info = rule.pattern().matchAndExtract(container);
            if (info != null) {
                return Optional.of(new Match(rule, info.getUriVariables()));
            }
        }
        return Optional.empty();
    }
}
