package com.viller.gespatiens.diary.infrastructure;

import com.viller.gespatiens.diary.domain.TherapeuticOuting;
import com.viller.gespatiens.diary.domain.TherapeuticOutingRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class TherapeuticOutingRepositoryJpaAdapter implements TherapeuticOutingRepository {

    private final TherapeuticOutingJpaRepository outingJpaRepository;

    public TherapeuticOutingRepositoryJpaAdapter(TherapeuticOutingJpaRepository outingJpaRepository) {
        this.outingJpaRepository = outingJpaRepository;
    }

    @Override
    public TherapeuticOuting save(TherapeuticOuting outing) {
        return toDomain(outingJpaRepository.save(toEntity(outing)));
    }

    @Override
    public Optional<TherapeuticOuting> findById(UUID id) {
        return outingJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<TherapeuticOuting> findByDate(LocalDate date) {
        return outingJpaRepository.findByDateOrderByLeftAt(date).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(TherapeuticOuting outing) {
        outingJpaRepository.deleteById(outing.getId());
    }

    private TherapeuticOutingJpaEntity toEntity(TherapeuticOuting outing) {
        TherapeuticOutingJpaEntity entity = new TherapeuticOutingJpaEntity();
        entity.setId(outing.getId());
        entity.setDate(outing.getDate());
        entity.setPatientId(outing.getPatientId());
        entity.setLeftAt(outing.getLeftAt());
        entity.setReturnedAt(outing.getReturnedAt().orElse(null));
        entity.setNotes(outing.getNotes().orElse(null));
        return entity;
    }

    private TherapeuticOuting toDomain(TherapeuticOutingJpaEntity entity) {
        return TherapeuticOuting.reconstitute(
                entity.getId(),
                entity.getDate(),
                entity.getPatientId(),
                entity.getLeftAt(),
                entity.getReturnedAt(),
                entity.getNotes());
    }
}
