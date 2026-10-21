package com.viller.gespatiens.patients.infrastructure;

import com.viller.gespatiens.patients.domain.AuthorizedVisitor;
import com.viller.gespatiens.patients.domain.AuthorizedVisitorRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class AuthorizedVisitorRepositoryJpaAdapter implements AuthorizedVisitorRepository {

    private final AuthorizedVisitorJpaRepository jpaRepository;

    public AuthorizedVisitorRepositoryJpaAdapter(AuthorizedVisitorJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AuthorizedVisitor save(AuthorizedVisitor visitor) {
        AuthorizedVisitorJpaEntity entity = new AuthorizedVisitorJpaEntity();
        entity.setId(visitor.getId());
        entity.setPatientId(visitor.getPatientId());
        entity.setName(visitor.getName());
        entity.setRelationship(visitor.getRelationship());
        entity.setPhoneNumber(visitor.getPhoneNumber());
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<AuthorizedVisitor> findById(UUID id) {
        return jpaRepository.findById(id).map(AuthorizedVisitorRepositoryJpaAdapter::toDomain);
    }

    @Override
    public List<AuthorizedVisitor> findByPatient(UUID patientId) {
        return jpaRepository.findByPatientIdOrderByNameAsc(patientId).stream()
                .map(AuthorizedVisitorRepositoryJpaAdapter::toDomain)
                .toList();
    }

    @Override
    public void delete(AuthorizedVisitor visitor) {
        jpaRepository.deleteById(visitor.getId());
    }

    private static AuthorizedVisitor toDomain(AuthorizedVisitorJpaEntity entity) {
        return AuthorizedVisitor.reconstitute(entity.getId(), entity.getPatientId(), entity.getName(),
                entity.getRelationship(), entity.getPhoneNumber());
    }
}
