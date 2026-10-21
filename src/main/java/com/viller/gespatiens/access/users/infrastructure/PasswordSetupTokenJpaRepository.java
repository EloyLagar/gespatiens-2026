package com.viller.gespatiens.access.users.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface PasswordSetupTokenJpaRepository extends JpaRepository<PasswordSetupTokenJpaEntity, String> {

    // Un solo DELETE en vez de leer los tokens y borrarlos uno a uno
    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordSetupTokenJpaEntity t WHERE t.userId = :userId")
    void deleteByUserId(UUID userId);
}
