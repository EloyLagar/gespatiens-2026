package com.viller.gespatiens.diary.infrastructure;

import com.viller.gespatiens.diary.domain.DayPart;
import com.viller.gespatiens.diary.domain.ShiftLog;
import com.viller.gespatiens.diary.domain.ShiftLogRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class ShiftLogRepositoryJpaAdapter implements ShiftLogRepository {

    private final ShiftLogJpaRepository shiftLogJpaRepository;

    public ShiftLogRepositoryJpaAdapter(ShiftLogJpaRepository shiftLogJpaRepository) {
        this.shiftLogJpaRepository = shiftLogJpaRepository;
    }

    @Override
    public ShiftLog save(ShiftLog shiftLog) {
        // saveAndFlush: el choque de versiones salta aquí y se traduce a un 409 claro
        return toDomain(shiftLogJpaRepository.saveAndFlush(toEntity(shiftLog)));
    }

    @Override
    public List<ShiftLog> findByDate(LocalDate date) {
        return shiftLogJpaRepository.findByDate(date).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<ShiftLog> findByDateAndPart(LocalDate date, DayPart part) {
        return shiftLogJpaRepository.findByDateAndPart(date, part).map(this::toDomain);
    }

    private ShiftLogJpaEntity toEntity(ShiftLog log) {
        ShiftLogJpaEntity entity = new ShiftLogJpaEntity();
        entity.setId(log.getId());
        entity.setDate(log.getDate());
        entity.setPart(log.getPart());
        entity.setStaffIds(log.getStaffIds());
        entity.setNotes(log.getNotes().orElse(null));
        entity.setUpdatedBy(log.getUpdatedBy().orElse(null));
        entity.setUpdatedAt(log.getUpdatedAt());
        entity.setRevision(log.getRevision().orElse(null));
        return entity;
    }

    private ShiftLog toDomain(ShiftLogJpaEntity entity) {
        return ShiftLog.reconstitute(
                entity.getId(),
                entity.getDate(),
                entity.getPart(),
                entity.getStaffIds(),
                entity.getNotes(),
                entity.getUpdatedBy(),
                entity.getUpdatedAt(),
                entity.getRevision());
    }
}
