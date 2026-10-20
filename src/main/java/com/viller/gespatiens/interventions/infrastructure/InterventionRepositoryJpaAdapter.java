package com.viller.gespatiens.interventions.infrastructure;

import com.viller.gespatiens.interventions.domain.Intervention;
import com.viller.gespatiens.interventions.domain.InterventionRepository;
import com.viller.gespatiens.interventions.domain.InterventionStats;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class InterventionRepositoryJpaAdapter implements InterventionRepository {

    private final InterventionJpaRepository interventionJpaRepository;

    public InterventionRepositoryJpaAdapter(InterventionJpaRepository interventionJpaRepository) {
        this.interventionJpaRepository = interventionJpaRepository;
    }

    @Override
    public Intervention save(Intervention intervention) {
        return toDomain(interventionJpaRepository.save(toEntity(intervention)));
    }

    @Override
    public Optional<Intervention> findById(UUID id) {
        return interventionJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Intervention> findByPatient(UUID patientId) {
        return interventionJpaRepository.findByPatientIdOrderByDateDescCreatedAtDesc(patientId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Map<UUID, InterventionStats> statsByPatient() {
        return interventionJpaRepository.statsByPatient().stream()
                .collect(Collectors.toMap(InterventionJpaRepository.PatientStats::getPatientId,
                        s -> new InterventionStats(s.getTotal(), s.getLatest())));
    }

    @Override
    public void delete(Intervention intervention) {
        interventionJpaRepository.deleteById(intervention.getId());
    }

    private InterventionJpaEntity toEntity(Intervention intervention) {
        InterventionJpaEntity entity = new InterventionJpaEntity();
        entity.setId(intervention.getId());
        entity.setPatientId(intervention.getPatientId());
        entity.setAuthorId(intervention.getAuthorId().orElse(null));
        entity.setDate(intervention.getDate());
        entity.setText(intervention.getText());
        entity.setCreatedAt(intervention.getCreatedAt());
        entity.setEditedAt(intervention.getEditedAt().orElse(null));
        return entity;
    }

    private Intervention toDomain(InterventionJpaEntity entity) {
        return Intervention.reconstitute(
                entity.getId(),
                entity.getPatientId(),
                entity.getAuthorId(),
                entity.getDate(),
                entity.getText(),
                entity.getCreatedAt(),
                entity.getEditedAt());
    }
}
