package com.viller.gespatiens.interventions.domain;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// Puerto hacia el módulo de empleados. La implementación (UsersStaffDirectory) vive en infrastructure
public interface StaffDirectory {
    Optional<StaffMember> find(UUID employeeId);
    // Varios empleados en una sola llamada; los que no existen no aparecen
    Map<UUID, StaffMember> findAll(Collection<UUID> employeeIds);
}
