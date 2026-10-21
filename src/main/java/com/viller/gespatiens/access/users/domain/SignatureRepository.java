package com.viller.gespatiens.access.users.domain;

import java.util.Optional;
import java.util.UUID;

public interface SignatureRepository {
    // Sustituye la que hubiera
    Signature save(Signature signature);
    Optional<Signature> findByUser(UUID userId);
    void deleteByUser(UUID userId);
}
