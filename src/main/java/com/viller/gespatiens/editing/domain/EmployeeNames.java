package com.viller.gespatiens.editing.domain;

import java.util.Optional;
import java.util.UUID;

// Puerto hacia el módulo de empleados: solo hace falta el nombre de quien edita
public interface EmployeeNames {
    Optional<String> nameOf(UUID employeeId);
}
