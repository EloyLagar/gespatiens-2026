package com.viller.gespatiens.access.users.domain;

import java.util.Optional;
import java.util.UUID;

public interface PasswordSetupTokenRepository {
    void save(PasswordSetupToken token);
    Optional<PasswordSetupToken> findByTokenHash(String tokenHash);
    // Un enlace nuevo anula los anteriores, y crear la contraseña anula todos
    void deleteByUserId(UUID userId);
}
