package com.viller.gespatiens.interventions.domain;

import java.util.Set;
import java.util.UUID;

// Los tutores de un residente (Tutors en la memoria): los empleados que le acompañan en el programa
// y los únicos que pueden registrar sus intervenciones
public class ResidentTutors {

    private final UUID patientId;
    private Set<UUID> tutorIds;

    private ResidentTutors(UUID patientId, Set<UUID> tutorIds) {
        if (patientId == null) {
            throw new IllegalArgumentException("Los tutores tienen que ser de un residente");
        }
        this.patientId = patientId;
        this.tutorIds = Set.copyOf(tutorIds == null ? Set.of() : tutorIds);
    }

    // Un residente sin tutores asignados todavía
    public static ResidentTutors none(UUID patientId) {
        return new ResidentTutors(patientId, Set.of());
    }

    public static ResidentTutors reconstitute(UUID patientId, Set<UUID> tutorIds) {
        return new ResidentTutors(patientId, tutorIds);
    }

    // Sustituye a todos los tutores. Vacío: el residente se queda sin tutores
    public void assign(Set<UUID> newTutorIds) {
        this.tutorIds = Set.copyOf(newTutorIds == null ? Set.of() : newTutorIds);
    }

    public boolean isTutor(UUID employeeId) {
        return tutorIds.contains(employeeId);
    }

    public UUID getPatientId() {
        return patientId;
    }

    public Set<UUID> getTutorIds() {
        return tutorIds;
    }
}
