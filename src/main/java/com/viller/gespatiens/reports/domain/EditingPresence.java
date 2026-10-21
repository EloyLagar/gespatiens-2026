package com.viller.gespatiens.reports.domain;

import java.util.UUID;

// Puerto hacia el módulo de bloqueos de edición (RF8): antes de guardar un apartado o la cabecera se comprueba
// que no lo esté editando otra persona. Si lo está, lanza la excepción de ese módulo (409 con su nombre)
public interface EditingPresence {

    void ensureNotEditedByOther(String resource, UUID employeeId);

    // Los nombres de lo que se bloquea dentro de un informe. La pantalla usa los mismos
    static String section(UUID reportId, ReportSection section) {
        return "report:" + reportId + ":section:" + section.name();
    }

    static String header(UUID reportId) {
        return "report:" + reportId + ":header";
    }
}
