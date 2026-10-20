package com.viller.gespatiens.interventions.domain;

import java.util.Map;
import java.util.UUID;

public interface ResidentTutorsRepository {
    ResidentTutors save(ResidentTutors tutors);
    // Un residente sin tutores devuelve ResidentTutors.none, nunca vacío
    ResidentTutors findByPatient(UUID patientId);
    // Solo los residentes que tienen algún tutor
    Map<UUID, ResidentTutors> findAll();
}
