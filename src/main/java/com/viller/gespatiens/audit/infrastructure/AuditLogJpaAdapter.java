package com.viller.gespatiens.audit.infrastructure;

import com.viller.gespatiens.audit.domain.AuditEntry;
import com.viller.gespatiens.audit.domain.AuditLog;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class AuditLogJpaAdapter implements AuditLog {

    private final AuditEntryJpaRepository jpaRepository;

    public AuditLogJpaAdapter(AuditEntryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void append(AuditEntry entry) {
        AuditEntryJpaEntity entity = new AuditEntryJpaEntity();
        entity.setId(entry.getId());
        entity.setOccurredAt(entry.getOccurredAt());
        entity.setActorId(entry.getActorId());
        entity.setAction(entry.getAction());
        entity.setPatientId(entry.getPatientId());
        entity.setResourceId(entry.getResourceId());
        jpaRepository.save(entity);
    }

    @Override
    public Page search(Filter filter, int page, int size) {
        Specification<AuditEntryJpaEntity> spec = Specification.allOf(
                equalTo("patientId", filter.patientId()),
                equalTo("actorId", filter.actorId()),
                from(filter.from()),
                before(filter.to()));
        org.springframework.data.domain.Page<AuditEntryJpaEntity> found = jpaRepository.findAll(spec,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt")));
        return new Page(found.map(AuditLogJpaAdapter::toDomain).toList(), page, found.getTotalPages(),
                found.getTotalElements());
    }

    // Un criterio a null no filtra: así la consulta no lleva parámetros null, que PostgreSQL no sabe tipar
    private static Specification<AuditEntryJpaEntity> equalTo(String field, UUID value) {
        return (root, query, cb) -> value == null ? null : cb.equal(root.get(field), value);
    }

    private static Specification<AuditEntryJpaEntity> from(Instant from) {
        return (root, query, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("occurredAt"), from);
    }

    private static Specification<AuditEntryJpaEntity> before(Instant to) {
        return (root, query, cb) -> to == null ? null : cb.lessThan(root.get("occurredAt"), to);
    }

    private static AuditEntry toDomain(AuditEntryJpaEntity e) {
        return AuditEntry.reconstitute(e.getId(), e.getOccurredAt(), e.getActorId(), e.getAction(), e.getPatientId(),
                e.getResourceId());
    }
}
