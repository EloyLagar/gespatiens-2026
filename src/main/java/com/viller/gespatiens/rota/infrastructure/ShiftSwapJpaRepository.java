package com.viller.gespatiens.rota.infrastructure;

import com.viller.gespatiens.rota.domain.SwapStatus;
import com.viller.gespatiens.rota.infrastructure.RotaJpaEntities.ShiftSwapJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ShiftSwapJpaRepository extends JpaRepository<ShiftSwapJpaEntity, UUID> {

    @Query("SELECT s FROM ShiftSwapJpaEntity s WHERE s.requesterId = :employee OR s.partnerId = :employee "
            + "ORDER BY s.createdAt DESC")
    List<ShiftSwapJpaEntity> findInvolving(UUID employee);

    @Query("SELECT s FROM ShiftSwapJpaEntity s WHERE s.status = :status "
            + "AND (s.requesterShiftId IN :shifts OR s.partnerShiftId IN :shifts)")
    List<ShiftSwapJpaEntity> findByStatusForShifts(SwapStatus status, Collection<UUID> shifts);
}
