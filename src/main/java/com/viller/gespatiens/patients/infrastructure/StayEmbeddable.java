package com.viller.gespatiens.patients.infrastructure;

import com.viller.gespatiens.patients.domain.Stay;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDate;

// Una fila de patient_stays: una estancia anterior, ya terminada
@Embeddable
public class StayEmbeddable {

    @Column(nullable = false)
    private LocalDate entryDate;

    @Column(nullable = false)
    private LocalDate exitDate;

    protected StayEmbeddable() {
    }

    static StayEmbeddable from(Stay stay) {
        StayEmbeddable embeddable = new StayEmbeddable();
        embeddable.entryDate = stay.entryDate();
        embeddable.exitDate = stay.exitDate();
        return embeddable;
    }

    Stay toDomain() {
        return new Stay(entryDate, exitDate);
    }
}
