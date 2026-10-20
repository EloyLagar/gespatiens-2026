package com.viller.gespatiens.interventions.infrastructure;

import com.viller.gespatiens.interventions.domain.ResidentTutors;
import com.viller.gespatiens.interventions.domain.ResidentTutorsRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ResidentTutorsRepositoryJpaAdapter implements ResidentTutorsRepository {

    private final TutorJpaRepository tutorJpaRepository;

    public ResidentTutorsRepositoryJpaAdapter(TutorJpaRepository tutorJpaRepository) {
        this.tutorJpaRepository = tutorJpaRepository;
    }

    // Sustituye las filas del residente por las nuevas, todo o nada
    @Override
    @Transactional
    public ResidentTutors save(ResidentTutors tutors) {
        tutorJpaRepository.deleteByPatientId(tutors.getPatientId());
        tutorJpaRepository.flush();
        tutorJpaRepository.saveAll(tutors.getTutorIds().stream()
                .map(userId -> new TutorJpaEntity(tutors.getPatientId(), userId))
                .toList());
        return ResidentTutors.reconstitute(tutors.getPatientId(), tutors.getTutorIds());
    }

    @Override
    public ResidentTutors findByPatient(UUID patientId) {
        return ResidentTutors.reconstitute(patientId, tutorJpaRepository.findByPatientId(patientId).stream()
                .map(TutorJpaEntity::getUserId)
                .collect(Collectors.toSet()));
    }

    @Override
    public Map<UUID, ResidentTutors> findAll() {
        return tutorJpaRepository.findAll().stream()
                .collect(Collectors.groupingBy(TutorJpaEntity::getPatientId,
                        Collectors.mapping(TutorJpaEntity::getUserId, Collectors.toSet())))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> ResidentTutors.reconstitute(e.getKey(), e.getValue())));
    }
}
