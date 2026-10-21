package com.viller.gespatiens.editing.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface EditLockJpaRepository extends JpaRepository<EditLockJpaEntity, String> {

    // Solo borra el bloqueo si sigue siendo de quien lo suelta: entre leerlo y borrarlo ha podido caducar
    // y quedárselo otra persona
    @Modifying
    @Transactional
    @Query("DELETE FROM EditLockJpaEntity l WHERE l.resource = :resource AND l.holderId = :holderId")
    void deleteHeld(String resource, UUID holderId);
}
