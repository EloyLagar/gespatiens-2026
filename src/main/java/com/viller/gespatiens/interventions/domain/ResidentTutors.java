package com.viller.gespatiens.interventions.domain;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// Los tutores de un residente (Tutors en la memoria): los empleados que le acompañan en el programa
// y los únicos que pueden registrar sus intervenciones. La memoria final: "estos son un educador social y un
// psicólogo". Se guardan como un conjunto (la tabla tutors), pero como mucho uno de cada
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

    // Sustituye a los dos tutores. null: ese tutor queda sin asignar
    public void assign(StaffMember educator, StaffMember psychologist) {
        if (educator != null && !educator.isSocialEducator()) {
            throw new IllegalArgumentException("El tutor educador tiene que ser educador social: " + educator.name()
                    + " no lo es");
        }
        if (psychologist != null && !psychologist.isPsychologist()) {
            throw new IllegalArgumentException("El tutor psicólogo tiene que ser psicólogo: " + psychologist.name()
                    + " no lo es");
        }
        Set<UUID> ids = new HashSet<>();
        if (educator != null) {
            ids.add(educator.id());
        }
        if (psychologist != null) {
            ids.add(psychologist.id());
        }
        this.tutorIds = Set.copyOf(ids);
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
