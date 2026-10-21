package com.viller.gespatiens.access.users.infrastructure;

import com.viller.gespatiens.access.users.domain.PasswordSetupToken;
import com.viller.gespatiens.access.users.domain.PasswordSetupTokenRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class PasswordSetupTokenRepositoryJpaAdapter implements PasswordSetupTokenRepository {

    private final PasswordSetupTokenJpaRepository jpaRepository;

    public PasswordSetupTokenRepositoryJpaAdapter(PasswordSetupTokenJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(PasswordSetupToken token) {
        PasswordSetupTokenJpaEntity entity = new PasswordSetupTokenJpaEntity();
        entity.setTokenHash(token.getTokenHash());
        entity.setUserId(token.getUserId());
        entity.setExpiresAt(token.getExpiresAt());
        jpaRepository.save(entity);
    }

    @Override
    public Optional<PasswordSetupToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findById(tokenHash)
                .map(e -> PasswordSetupToken.reconstitute(e.getTokenHash(), e.getUserId(), e.getExpiresAt()));
    }

    @Override
    public void deleteByUserId(UUID userId) {
        jpaRepository.deleteByUserId(userId);
    }
}
