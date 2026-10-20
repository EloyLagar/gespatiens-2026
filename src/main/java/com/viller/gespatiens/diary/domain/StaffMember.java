package com.viller.gespatiens.diary.domain;

import java.util.UUID;

// Lo que el diario necesita saber de un empleado
public record StaffMember(UUID id, String name, String speciality) {
}
