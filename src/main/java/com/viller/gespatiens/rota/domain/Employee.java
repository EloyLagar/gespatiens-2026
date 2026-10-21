package com.viller.gespatiens.rota.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Lo que el cuadrante necesita saber de un trabajador
public record Employee(UUID id, String name, String speciality) {

    // Puerto hacia el módulo de empleados. La implementación (RotaStaffDirectory) vive en infrastructure
    public interface Directory {
        Optional<Employee> find(UUID id);
        // Toda la plantilla: en el cuadrante están todos los trabajadores (decisión del centro)
        List<Employee> findAll();
    }
}
