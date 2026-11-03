package com.viller.gespatiens.audit.domain;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// Repositorio del registro de accesos: se añade, se consulta y se borra lo que ha pasado el plazo
public interface AccessLog {

    void append(AccessEntry entry);

    // Borra lo anterior a limit (excluido). Devuelve cuántas líneas
    long deleteBefore(Instant limit);

    // Del más reciente al más antiguo. page empieza en 0
    Page search(Filter filter, int page, int size);

    // userId null u outcomes vacío no filtran. from incluido, to excluido
    record Filter(UUID userId, Set<AccessOutcome> outcomes, Instant from, Instant to) {
        public Filter {
            outcomes = outcomes == null ? Set.of() : Set.copyOf(outcomes);
        }
    }

    record Page(List<AccessEntry> entries, int page, int totalPages, long totalEntries) {
    }
}
