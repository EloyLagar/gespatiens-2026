package com.viller.gespatiens.access.users.infrastructure;

import com.viller.gespatiens.access.users.domain.Signature;
import com.viller.gespatiens.access.users.domain.SignatureRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class SignatureRepositoryJpaAdapter implements SignatureRepository {

    private final SignatureJpaRepository jpaRepository;

    public SignatureRepositoryJpaAdapter(SignatureJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Signature save(Signature signature) {
        SignatureJpaEntity entity = new SignatureJpaEntity();
        entity.setUserId(signature.getUserId());
        entity.setImage(signature.getPng());
        entity.setUploadedAt(signature.getUploadedAt());
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Signature> findByUser(UUID userId) {
        return jpaRepository.findById(userId).map(SignatureRepositoryJpaAdapter::toDomain);
    }

    @Override
    public void deleteByUser(UUID userId) {
        jpaRepository.deleteById(userId);
    }

    private static Signature toDomain(SignatureJpaEntity entity) {
        return Signature.reconstitute(entity.getUserId(), entity.getImage(), entity.getUploadedAt());
    }
}
