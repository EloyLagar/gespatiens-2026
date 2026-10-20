package com.viller.gespatiens.interventions.domain;

import java.util.UUID;

// Lo que las intervenciones necesitan saber de un empleado
public record StaffMember(UUID id, String name, String speciality, boolean administrator) {
}
