package com.viller.gespatiens.audit.infrastructure.web;

import com.viller.gespatiens.audit.application.SearchAccessLogUseCase;
import com.viller.gespatiens.audit.domain.AccessOutcome;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// El registro de accesos. Solo la administración (SecurityConfig: /api/audit/**)
@RestController
@RequestMapping("/api/audit/access")
public class AccessLogController {

    private final SearchAccessLogUseCase searchAccessLog;

    public AccessLogController(SearchAccessLogUseCase searchAccessLog) {
        this.searchAccessLog = searchAccessLog;
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

    // employee null: el email no es de nadie. employee.name null: ya no está en la plantilla
    public record AccessEntryResponse(UUID id, Instant occurredAt, String email, EmployeeResponse employee,
                                      AccessOutcome outcome, String ipAddress, String userAgent) {
        static AccessEntryResponse from(SearchAccessLogUseCase.Line line) {
            var e = line.entry();
            return new AccessEntryResponse(e.getId(), e.getOccurredAt(), e.getEmail(),
                    e.getUserId() == null ? null : new EmployeeResponse(e.getUserId(), line.employeeName()),
                    e.getOutcome(), e.getIpAddress(), e.getUserAgent());
        }
    }

    public record EmployeeResponse(UUID id, String name) {
    }

    public record AccessPageResponse(List<AccessEntryResponse> entries, int page, int totalPages,
                                     long totalEntries) {
    }
}
