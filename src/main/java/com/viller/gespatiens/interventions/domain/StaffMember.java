package com.viller.gespatiens.interventions.domain;

import java.util.UUID;

// Lo que las intervenciones necesitan saber de un empleado
public record StaffMember(UUID id, String name, String speciality, boolean administrator) {

    // Los tutores son siempre educadores sociales (decisión del centro)
    public boolean isSocialEducator() {
        return "SOCIAL_EDUCATOR".equals(speciality);
    }
}
