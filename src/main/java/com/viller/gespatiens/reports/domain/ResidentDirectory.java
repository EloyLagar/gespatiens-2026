package com.viller.gespatiens.reports.domain;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// Puerto hacia el módulo de residentes. La implementación (ReportsResidentDirectory) vive en infrastructure
public interface ResidentDirectory {
    Optional<Resident> find(UUID patientId);
    // Varios residentes en una sola llamada; los que no existen no aparecen
    Map<UUID, Resident> findAll(Collection<UUID> patientIds);
    // Los que están en el centro
    List<Resident> findActive();
}
