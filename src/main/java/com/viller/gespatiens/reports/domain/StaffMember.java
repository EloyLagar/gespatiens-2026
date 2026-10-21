package com.viller.gespatiens.reports.domain;

import java.util.UUID;

// Lo que los informes necesitan saber de un empleado
public record StaffMember(UUID id, String name, String speciality, boolean administrator) {
}
