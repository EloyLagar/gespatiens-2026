package com.viller.gespatiens.audit.application;

import com.viller.gespatiens.audit.domain.AccessEntry;
import com.viller.gespatiens.audit.domain.AccessLog;
import com.viller.gespatiens.audit.domain.AccessOutcome;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

// La administración consulta quién ha entrado y quién lo ha intentado sin conseguirlo: las entradas de un
// empleado, los fallos y los bloqueos, en unas fechas. Una página cada vez, de lo más reciente a lo más antiguo
public class SearchAccessLogUseCase {

    public static final int PAGE_SIZE = 50;
    private static final ZoneId CENTER_ZONE = ZoneId.of("Europe/Madrid");

    private final AccessLog accessLog;
    private final AuditDirectory directory;

    public SearchAccessLogUseCase(AccessLog accessLog, AuditDirectory directory) {
        this.accessLog = accessLog;
        this.directory = directory;
    }

    public Result execute(UUID userId, Set<AccessOutcome> outcomes, LocalDate from, LocalDate to, int page) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la inicial");
        }
        if (page < 0) {
            throw new IllegalArgumentException("La página no puede ser negativa");
        }
        AccessLog.Filter filter = new AccessLog.Filter(userId, outcomes,
                from == null ? null : from.atStartOfDay(CENTER_ZONE).toInstant(),
                to == null ? null : to.plusDays(1).atStartOfDay(CENTER_ZONE).toInstant());
        AccessLog.Page found = accessLog.search(filter, page, PAGE_SIZE);

        Map<UUID, String> names = directory.employeeNames(
                found.entries().stream().map(AccessEntry::getUserId).filter(Objects::nonNull).distinct().toList());
        List<Line> lines = found.entries().stream()
                .map(e -> new Line(e, e.getUserId() == null ? null : names.get(e.getUserId())))
                .toList();
        return new Result(lines, found.page(), found.totalPages(), found.totalEntries());
    }

    // employeeName null: el email no era de nadie, o el empleado ya no está en la plantilla
    public record Line(AccessEntry entry, String employeeName) {
    }

    public record Result(List<Line> lines, int page, int totalPages, long totalEntries) {
    }
}
