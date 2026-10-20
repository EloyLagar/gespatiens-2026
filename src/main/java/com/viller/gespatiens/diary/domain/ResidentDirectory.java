package com.viller.gespatiens.diary.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Puerto hacia el módulo de residentes. La implementación (PatientsResidentDirectory) vive en infrastructure
public interface ResidentDirectory {
    Optional<Resident> find(UUID patientId);
    // Los que están en el centro, por número de plaza: son quienes aparecen al pasar lista
    List<Resident> findActive();
}
