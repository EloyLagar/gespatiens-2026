package com.viller.gespatiens.diary.domain;

import java.util.Optional;
import java.util.UUID;

// Puerto hacia el módulo de residentes. La implementación (PatientsResidentDirectory) vive en infrastructure
public interface ResidentDirectory {
    Optional<Resident> find(UUID patientId);
}
