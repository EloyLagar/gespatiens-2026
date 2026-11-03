package com.viller.gespatiens.audit.infrastructure;

import com.viller.gespatiens.audit.domain.AccessEntry;
import com.viller.gespatiens.audit.domain.AccessLog;
import com.viller.gespatiens.audit.domain.AccessOutcome;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Component
public class AccessLogJpaAdapter implements AccessLog {

    private final AccessEntryJpaRepository jpaRepository;

    public AccessLogJpaAdapter(AccessEntryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void append(AccessEntry entry) {
        AccessEntryJpaEntity entity = new AccessEntryJpaEntity();
        entity.setId(entry.getId());
        entity.setOccurredAt(entry.getOccurredAt());
        entity.setEmail(entry.getEmail());
        entity.setUserId(entry.getUserId());
        entity.setActorId(entry.getActorId());
        entity.setOutcome(entry.getOutcome());
        entity.setIpAddress(entry.getIpAddress());
        entity.setUserAgent(entry.getUserAgent());
        jpaRepository.save(entity);
    }

    @Override
    public long deleteBefore(Instant limit) {
        return jpaRepository.deleteOlderThan(limit);
    }

    @Override
    public Page search(Filter filter, int page, int size) {
        Specification<AccessEntryJpaEntity> spec = Specification.allOf(
                userIs(filter.userId()),
                outcomeIn(filter.outcomes()),
                from(filter.from()),
                before(filter.to()));
        org.springframework.data.domain.Page<AccessEntryJpaEntity> found = jpaRepository.findAll(spec,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt")));
        return new Page(found.map(AccessLogJpaAdapter::toDomain).toList(), page, found.getTotalPages(),
                found.getTotalElements());
    }

    // Un criterio a null no filtra: así la consulta no lleva parámetros null, que PostgreSQL no sabe tipar
    private static Specification<AccessEntryJpaEntity> userIs(UUID userId) {
        return (root, query, cb) -> userId == null ? null : cb.equal(root.get("userId"), userId);
    }

    private static Specification<AccessEntryJpaEntity> outcomeIn(Set<AccessOutcome> outcomes) {
        return (root, query, cb) -> outcomes.isEmpty() ? null : root.get("outcome").in(outcomes);
    }

    private static Specification<AccessEntryJpaEntity> from(Instant from) {
        return (root, query, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("occurredAt"), from);
    }

    private static Specification<AccessEntryJpaEntity> before(Instant to) {
        return (root, query, cb) -> to == null ? null : cb.lessThan(root.get("occurredAt"), to);
    }

    private static AccessEntry toDomain(AccessEntryJpaEntity e) {
        return AccessEntry.reconstitute(e.getId(), e.getOccurredAt(), e.getEmail(), e.getUserId(), e.getActorId(), e.getOutcome(),
                e.getIpAddress(), e.getUserAgent());
    }
}
