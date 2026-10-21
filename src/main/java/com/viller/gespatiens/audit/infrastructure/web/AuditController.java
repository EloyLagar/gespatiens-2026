package com.viller.gespatiens.audit.infrastructure.web;

import com.viller.gespatiens.audit.application.AuditDirectory;
import com.viller.gespatiens.audit.application.SearchAuditLogUseCase;
import com.viller.gespatiens.audit.domain.AuditAction;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// El registro de accesos. Solo la administración (SecurityConfig)
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final SearchAuditLogUseCase searchAuditLog;

    public AuditController(SearchAuditLogUseCase searchAuditLog) {
        this.searchAuditLog = searchAuditLog;
    }

    // ?patientId=&actorId=&from=2026-10-01&to=2026-10-31&page=0. Todo opcional
    @GetMapping
    public AuditPageResponse search(@RequestParam(required = false) UUID patientId,
                                    @RequestParam(required = false) UUID actorId,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                    @RequestParam(defaultValue = "0") int page) {
        SearchAuditLogUseCase.Result result = searchAuditLog.execute(patientId, actorId, from, to, page);
        return new AuditPageResponse(result.lines().stream().map(AuditEntryResponse::from).toList(), result.page(),
                result.totalPages(), result.totalEntries());
    }

    // actor.name null: ya no está en la plantilla. resident null: sin residente; resident.name null: borrado
    public record AuditEntryResponse(UUID id, Instant occurredAt, AuditAction action, ActorResponse actor,
                                     ResidentResponse resident, UUID resourceId) {
        static AuditEntryResponse from(SearchAuditLogUseCase.Line line) {
            var e = line.entry();
            ResidentResponse resident = null;
            if (e.getPatientId() != null) {
                AuditDirectory.Resident r = line.resident();
                resident = r == null
                        ? new ResidentResponse(e.getPatientId(), null, null, null)
                        : new ResidentResponse(r.id(), r.number(), r.name(), r.surname());
            }
            return new AuditEntryResponse(e.getId(), e.getOccurredAt(), e.getAction(),
                    new ActorResponse(e.getActorId(), line.actorName()), resident, e.getResourceId());
        }
    }

    public record ActorResponse(UUID id, String name) {
    }

    public record ResidentResponse(UUID id, Integer number, String name, String surname) {
    }

    public record AuditPageResponse(List<AuditEntryResponse> entries, int page, int totalPages, long totalEntries) {
    }
}
