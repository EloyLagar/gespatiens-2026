package com.viller.gespatiens.diary.infrastructure;

import com.viller.gespatiens.diary.domain.Reduction;
import com.viller.gespatiens.diary.domain.ReductionRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ReductionRepositoryJpaAdapter implements ReductionRepository {

    private final ReductionJpaRepository reductionJpaRepository;

    public ReductionRepositoryJpaAdapter(ReductionJpaRepository reductionJpaRepository) {
        this.reductionJpaRepository = reductionJpaRepository;
    }

    @Override
    public Reduction save(Reduction reduction) {
        return toDomain(reductionJpaRepository.save(toEntity(reduction)));
    }

    @Override
    public List<Reduction> findByDate(LocalDate date) {
        return reductionJpaRepository.findByDate(date).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Reduction> findByDateAndPatient(LocalDate date, UUID patientId) {
        return reductionJpaRepository.findByDateAndPatientId(date, patientId).map(this::toDomain);
    }

    @Override
    public void delete(Reduction reduction) {
        reductionJpaRepository.deleteById(reduction.getId());
    }

    private ReductionJpaEntity toEntity(Reduction reduction) {
        ReductionJpaEntity entity = new ReductionJpaEntity();
        entity.setId(reduction.getId());
        entity.setDate(reduction.getDate());
        entity.setPatientId(reduction.getPatientId());
        entity.setPartial(reduction.getPartial().orElse(null));
        entity.setTotal(reduction.isTotal());
        entity.setSport(reduction.isSport());
        entity.setRecordedBy(reduction.getRecordedBy().orElse(null));
        entity.setRecordedAt(reduction.getRecordedAt());
        return entity;
    }

    private Reduction toDomain(ReductionJpaEntity entity) {
        return Reduction.reconstitute(
                entity.getId(),
                entity.getDate(),
                entity.getPatientId(),
                entity.getPartial(),
                entity.isTotal(),
                entity.isSport(),
                entity.getRecordedBy(),
                entity.getRecordedAt());
    }
}
