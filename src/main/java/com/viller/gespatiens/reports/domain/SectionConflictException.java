package com.viller.gespatiens.reports.domain;

import java.util.Optional;

// RF8: alguien guardó el apartado mientras otra persona lo tenía abierto. En lugar de borrar lo que
// escribió el primero, se rechaza el segundo guardado y se le enseña lo que hay ahora
public class SectionConflictException extends RuntimeException {

    private final ReportSection section;
    // Vacío si el otro empleado lo dejó en blanco
    private final transient SectionEntry current;

    public SectionConflictException(ReportSection section, SectionEntry current) {
        super("Otra persona ha modificado \"" + section.label() + "\" mientras lo editabas. "
                + "Revisa su versión antes de guardar la tuya");
        this.section = section;
        this.current = current;
    }

    public ReportSection getSection() {
        return section;
    }

    public Optional<SectionEntry> getCurrent() {
        return Optional.ofNullable(current);
    }
}
