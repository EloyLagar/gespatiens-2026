package com.viller.gespatiens.audit.application;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// Puerto: los nombres que se enseñan junto al registro y de qué residente es un informe o una intervención.
// Lo implementa la infraestructura consultando las tablas de los otros módulos
public interface AuditDirectory {

    // Solo los que siguen existiendo: el empleado o el residente borrado no aparece
    Map<UUID, String> employeeNames(Collection<UUID> ids);

    Map<UUID, Resident> residents(Collection<UUID> ids);

    Optional<UUID> residentOfReport(UUID reportId);

    Optional<UUID> residentOfIntervention(UUID interventionId);

    record Resident(UUID id, Integer number, String name, String surname) {
    }
}
