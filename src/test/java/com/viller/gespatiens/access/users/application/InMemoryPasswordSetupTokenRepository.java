package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.domain.PasswordSetupToken;
import com.viller.gespatiens.access.users.domain.PasswordSetupTokenRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class InMemoryPasswordSetupTokenRepository implements PasswordSetupTokenRepository {

    final Map<String, PasswordSetupToken> tokens = new HashMap<>();

    @Override
    public void save(PasswordSetupToken token) {
        tokens.put(token.getTokenHash(), token);
    }

    @Override
    public Optional<PasswordSetupToken> findByTokenHash(String tokenHash) {
        return Optional.ofNullable(tokens.get(tokenHash));
    }

    @Override
    public void deleteByUserId(UUID userId) {
        tokens.values().removeIf(t -> t.getUserId().equals(userId));
    }
}
