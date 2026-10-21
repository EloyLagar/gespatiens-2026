package com.viller.gespatiens.notices.domain;

import java.util.UUID;

// Lo que el módulo de avisos necesita saber de un empleado. Los avisos no dependen del agregado User
// del módulo de empleados: lo traduce AuthorDirectory
public record Author(UUID id, String name, String speciality, boolean administrator) {
}
