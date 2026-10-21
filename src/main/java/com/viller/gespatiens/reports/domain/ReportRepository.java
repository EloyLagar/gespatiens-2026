package com.viller.gespatiens.reports.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportRepository {
    Report save(Report report);
    // Crea el informe de una estancia salvo que otra petición lo haya creado a la vez (no hace nada)
    void addIfMissing(Report report);
    Optional<Report> findById(UUID id);
    // Los de un residente, del más antiguo al más reciente
    List<Report> findByPatient(UUID patientId);
    List<Report> findAll();
}
