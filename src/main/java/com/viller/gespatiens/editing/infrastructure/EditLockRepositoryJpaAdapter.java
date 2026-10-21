package com.viller.gespatiens.editing.infrastructure;

import com.viller.gespatiens.editing.domain.EditLock;
import com.viller.gespatiens.editing.domain.EditLockRepository;
import com.viller.gespatiens.editing.domain.LockRaceException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class EditLockRepositoryJpaAdapter implements EditLockRepository {

    private final EditLockJpaRepository jpaRepository;

    public EditLockRepositoryJpaAdapter(EditLockJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<EditLock> find(String resource) {
        return jpaRepository.findById(resource).map(EditLockRepositoryJpaAdapter::toDomain);
    }

    @Override
    public EditLock save(EditLock lock) {
        try {
            return toDomain(jpaRepository.saveAndFlush(toEntity(lock)));
        } catch (DataIntegrityViolationException | OptimisticLockingFailureException e) {
            // INSERT duplicado o UPDATE con una revisión vieja: otra petición ha ganado
            throw new LockRaceException(e);
        }
    }

    @Override
    public void delete(EditLock lock) {
        jpaRepository.deleteHeld(lock.getResource(), lock.getHolderId());
    }

    @Override
    public List<EditLock> findAll() {
        return jpaRepository.findAll().stream().map(EditLockRepositoryJpaAdapter::toDomain).toList();
    }

    private static EditLockJpaEntity toEntity(EditLock lock) {
        EditLockJpaEntity entity = new EditLockJpaEntity();
        entity.setResource(lock.getResource());
        entity.setHolderId(lock.getHolderId());
        entity.setAcquiredAt(lock.getAcquiredAt());
        entity.setExpiresAt(lock.getExpiresAt());
        entity.setRevision(lock.getRevision().orElse(null));
        return entity;
    }

    private static EditLock toDomain(EditLockJpaEntity entity) {
        return EditLock.reconstitute(entity.getResource(), entity.getHolderId(), entity.getAcquiredAt(),
                entity.getExpiresAt(), entity.getRevision());
    }
}
