package com.viller.gespatiens.reports.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;
import java.util.UUID;

// Una fila de report_sections sin sus claves (las pone ReportJpaEntity)
@Embeddable
public class SectionEmbeddable {

    @Column(nullable = false, length = 4000)
    private String text;

    // Solo el id: los informes no cargan la entidad del empleado (otro módulo)
    private UUID authorId;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private int version;

    protected SectionEmbeddable() {
    }

    public SectionEmbeddable(String text, UUID authorId, Instant updatedAt, int version) {
        this.text = text;
        this.authorId = authorId;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public String getText() {
        return text;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public int getVersion() {
        return version;
    }
}
