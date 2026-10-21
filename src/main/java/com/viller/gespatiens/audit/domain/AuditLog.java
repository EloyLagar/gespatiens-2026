package com.viller.gespatiens.audit.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Repositorio del registro de cambios: se añade, se consulta y se borra lo que ha pasado el plazo
public interface AuditLog {

    void append(AuditEntry entry);

    // Borra lo anterior a limit (excluido). Devuelve cuántas líneas
    long deleteBefore(Instant limit);

    // Del más reciente al más antiguo. page empieza en 0
    Page search(Filter filter, int page, int size);

    // Cualquier criterio a null no filtra. from incluido, to excluido
    record Filter(UUID patientId, UUID actorId, Instant from, Instant to) {
    }

    record Page(List<AuditEntry> entries, int page, int totalPages, long totalEntries) {
    }
}
