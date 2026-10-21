package com.viller.gespatiens.audit.application;

import com.viller.gespatiens.audit.domain.AuditEntry;
import com.viller.gespatiens.audit.domain.AuditLog;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

// La administración consulta quién ha cambiado los datos de un residente, o qué ha cambiado un empleado,
// en unas fechas. Una página cada vez, de lo más reciente a lo más antiguo
public class SearchAuditLogUseCase {

    public static final int PAGE_SIZE = 50;
    // Las fechas del filtro son días del centro
    private static final ZoneId CENTER_ZONE = ZoneId.of("Europe/Madrid");

    private final AuditLog auditLog;
    private final AuditDirectory directory;

    public SearchAuditLogUseCase(AuditLog auditLog, AuditDirectory directory) {
        this.auditLog = auditLog;
        this.directory = directory;
    }

    public Result execute(UUID patientId, UUID actorId, LocalDate from, LocalDate to, int page) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la inicial");
        }
        if (page < 0) {
            throw new IllegalArgumentException("La página no puede ser negativa");
        }
        AuditLog.Filter filter = new AuditLog.Filter(patientId, actorId,
                from == null ? null : from.atStartOfDay(CENTER_ZONE).toInstant(),
                to == null ? null : to.plusDays(1).atStartOfDay(CENTER_ZONE).toInstant());
        AuditLog.Page found = auditLog.search(filter, page, PAGE_SIZE);

        Map<UUID, String> names = directory.employeeNames(
                found.entries().stream().map(AuditEntry::getActorId).distinct().toList());
        Map<UUID, AuditDirectory.Resident> residents = directory.residents(
                found.entries().stream().map(AuditEntry::getPatientId).filter(Objects::nonNull).distinct().toList());
        List<Line> lines = found.entries().stream()
                .map(e -> new Line(e, names.get(e.getActorId()),
                        e.getPatientId() == null ? null : residents.get(e.getPatientId())))
                .toList();
        return new Result(lines, found.page(), found.totalPages(), found.totalEntries());
    }

    // actorName null: el empleado ya no está en la plantilla. resident null: borrado o desconocido
    public record Line(AuditEntry entry, String actorName, AuditDirectory.Resident resident) {
    }

    public record Result(List<Line> lines, int page, int totalPages, long totalEntries) {
    }
}
