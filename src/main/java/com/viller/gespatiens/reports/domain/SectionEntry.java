package com.viller.gespatiens.reports.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

// Lo escrito en un apartado: el texto, quién lo guardó por última vez (null si ya no está en la plantilla),
// cuándo y cuántas veces se ha guardado (RF8)
public record SectionEntry(String text, UUID authorId, Instant updatedAt, int version) {

    public static final int MAX_LENGTH = 4000;

    public SectionEntry {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Un apartado guardado tiene que tener texto");
        }
        if (text.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Un apartado no puede superar los " + MAX_LENGTH + " caracteres");
        }
        if (updatedAt == null) {
            throw new IllegalArgumentException("Falta la fecha del apartado");
        }
        if (version < 1) {
            throw new IllegalArgumentException("La versión de un apartado empieza en 1");
        }
    }

    public Optional<UUID> author() {
        return Optional.ofNullable(authorId);
    }
}
