package com.viller.gespatiens.audit.infrastructure.web;

import com.viller.gespatiens.audit.application.MyLastAccessUseCase;
import com.viller.gespatiens.audit.application.SearchAccessLogUseCase;
import com.viller.gespatiens.audit.domain.AccessOutcome;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// El registro de accesos. Solo la administración (SecurityConfig: /api/audit/**), menos /mine: el acceso anterior
// de quien pregunta, que lo ve cada empleado
@RestController
@RequestMapping("/api/audit/access")
public class AccessLogController {

    private final SearchAccessLogUseCase searchAccessLog;
    private final MyLastAccessUseCase myLastAccess;

    public AccessLogController(SearchAccessLogUseCase searchAccessLog, MyLastAccessUseCase myLastAccess) {
        this.searchAccessLog = searchAccessLog;
        this.myLastAccess = myLastAccess;
    }

    // Lo que se le enseña a cada empleado al entrar. CurrentUserJwtConverter pone su id como "name"
    @GetMapping("/mine")
    public MyLastAccessResponse mine(Principal principal) {
        MyLastAccessUseCase.Result result = myLastAccess.execute(UUID.fromString(principal.getName()));
        var p = result.previous();
        return new MyLastAccessResponse(
                p == null ? null : new PreviousAccessResponse(p.getOccurredAt(), p.getIpAddress(), p.getUserAgent()),
                result.failedAttemptsSince());
    }

    // previous null: es su primera entrada registrada
    public record MyLastAccessResponse(PreviousAccessResponse previous, long failedAttemptsSince) {
    }

    public record PreviousAccessResponse(Instant occurredAt, String ipAddress, String userAgent) {
    }

    // ?userId=&outcome=FAILURE&outcome=LOCKED&from=2026-10-01&to=2026-10-31&page=0. Todo opcional; outcome se
    // puede repetir
    @GetMapping
    public AccessPageResponse search(@RequestParam(required = false) UUID userId,
                                     @RequestParam(required = false) Set<AccessOutcome> outcome,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                     @RequestParam(defaultValue = "0") int page) {
        SearchAccessLogUseCase.Result result = searchAccessLog.execute(userId, outcome, from, to, page);
        return new AccessPageResponse(result.lines().stream().map(AccessEntryResponse::from).toList(),
                result.page(), result.totalPages(), result.totalEntries());
    }

    // employee null: el email no es de nadie. employee.name null: ya no está en la plantilla. actor: quién lo hizo,
    // si no fue el propio empleado (la administración que pone una contraseña provisional)
    public record AccessEntryResponse(UUID id, Instant occurredAt, String email, EmployeeResponse employee,
                                      EmployeeResponse actor, AccessOutcome outcome, String ipAddress,
                                      String userAgent) {
        static AccessEntryResponse from(SearchAccessLogUseCase.Line line) {
            var e = line.entry();
            return new AccessEntryResponse(e.getId(), e.getOccurredAt(), e.getEmail(),
                    e.getUserId() == null ? null : new EmployeeResponse(e.getUserId(), line.employeeName()),
                    e.getActorId() == null ? null : new EmployeeResponse(e.getActorId(), line.actorName()),
                    e.getOutcome(), e.getIpAddress(), e.getUserAgent());
        }
    }

    public record EmployeeResponse(UUID id, String name) {
    }

    public record AccessPageResponse(List<AccessEntryResponse> entries, int page, int totalPages,
                                     long totalEntries) {
    }
}
