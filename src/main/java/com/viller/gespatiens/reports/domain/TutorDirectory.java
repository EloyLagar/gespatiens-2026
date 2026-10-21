package com.viller.gespatiens.reports.domain;

import java.util.UUID;

// Puerto hacia el módulo de intervenciones: los tutores de cada residente redactan las conclusiones del informe
public interface TutorDirectory {
    boolean isTutor(UUID patientId, UUID employeeId);
}
