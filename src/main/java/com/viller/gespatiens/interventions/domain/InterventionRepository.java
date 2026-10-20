package com.viller.gespatiens.interventions.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface InterventionRepository {
    Intervention save(Intervention intervention);
    Optional<Intervention> findById(UUID id);
    // Las de un residente, de la más reciente a la más antigua
    List<Intervention> findByPatient(UUID patientId);
    // Cuántas tiene cada residente y la fecha de la última, para el listado
    Map<UUID, InterventionStats> statsByPatient();
    void delete(Intervention intervention);
}
