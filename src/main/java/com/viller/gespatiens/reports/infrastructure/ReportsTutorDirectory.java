package com.viller.gespatiens.reports.infrastructure;

import com.viller.gespatiens.interventions.domain.ResidentTutorsRepository;
import com.viller.gespatiens.reports.domain.TutorDirectory;
import org.springframework.stereotype.Component;

import java.util.UUID;

// Adaptador entre módulos: los tutores que asigna el módulo de intervenciones
@Component
public class ReportsTutorDirectory implements TutorDirectory {

    private final ResidentTutorsRepository tutorsRepository;

    public ReportsTutorDirectory(ResidentTutorsRepository tutorsRepository) {
        this.tutorsRepository = tutorsRepository;
    }

    @Override
    public boolean isTutor(UUID patientId, UUID employeeId) {
        return tutorsRepository.findByPatient(patientId).isTutor(employeeId);
    }
}
