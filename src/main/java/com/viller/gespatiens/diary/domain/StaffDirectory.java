package com.viller.gespatiens.diary.domain;

import java.util.Optional;
import java.util.UUID;

// Puerto hacia el módulo de empleados. La implementación (UsersStaffDirectory) vive en infrastructure
public interface StaffDirectory {
    Optional<StaffMember> find(UUID employeeId);
}
