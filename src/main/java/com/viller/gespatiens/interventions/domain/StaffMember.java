package com.viller.gespatiens.interventions.domain;

import java.util.UUID;

// Lo que las intervenciones necesitan saber de un empleado
public record StaffMember(UUID id, String name, String speciality, boolean administrator) {

    // Cada residente tiene un tutor educador social y un tutor psicólogo (memoria final)
    public boolean isSocialEducator() {
        return "SOCIAL_EDUCATOR".equals(speciality);
    }

    public boolean isPsychologist() {
        return "PSYCHOLOGIST".equals(speciality);
    }
}
