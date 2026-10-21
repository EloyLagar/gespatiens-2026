package com.viller.gespatiens.rota.infrastructure;

import com.viller.gespatiens.rota.domain.PlannedShift;
import com.viller.gespatiens.rota.domain.RotaRepositories.PlannedShiftRepository;
import com.viller.gespatiens.rota.domain.RotaRepositories.ShiftSwapRepository;
import com.viller.gespatiens.rota.domain.ShiftPart;
import com.viller.gespatiens.rota.domain.ShiftSwap;
import com.viller.gespatiens.rota.domain.SwapStatus;
import com.viller.gespatiens.rota.infrastructure.RotaJpaEntities.PlannedShiftJpaEntity;
import com.viller.gespatiens.rota.infrastructure.RotaJpaEntities.ShiftSwapJpaEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Los adaptadores JPA del cuadrante
public final class RotaRepositoriesJpaAdapter {

    private RotaRepositoriesJpaAdapter() {
    }

    @Component
    public static class PlannedShifts implements PlannedShiftRepository {

        private final PlannedShiftJpaRepository jpa;

        public PlannedShifts(PlannedShiftJpaRepository jpa) {
            this.jpa = jpa;
        }

        @Override
        public PlannedShift save(PlannedShift s) {
            return toDomain(jpa.save(new PlannedShiftJpaEntity(s.getId(), s.getDate(), s.getPart(), s.getEmployeeId())));
        }

        @Override
        public Optional<PlannedShift> findById(UUID id) {
            return jpa.findById(id).map(PlannedShifts::toDomain);
        }

        @Override
        public List<PlannedShift> findBetween(LocalDate from, LocalDate to) {
            return jpa.findByDateBetweenOrderByDateAscPartAsc(from, to).stream().map(PlannedShifts::toDomain).toList();
        }

        @Override
        public boolean exists(LocalDate date, ShiftPart part, UUID employeeId) {
            return jpa.existsByDateAndPartAndEmployeeId(date, part, employeeId);
        }

        @Override
        public void delete(PlannedShift shift) {
            jpa.deleteById(shift.getId());
        }

        private static PlannedShift toDomain(PlannedShiftJpaEntity e) {
            return PlannedShift.reconstitute(e.getId(), e.getDate(), e.getPart(), e.getEmployeeId());
        }
    }

    @Component
    public static class ShiftSwaps implements ShiftSwapRepository {

        private final ShiftSwapJpaRepository jpa;

        public ShiftSwaps(ShiftSwapJpaRepository jpa) {
            this.jpa = jpa;
        }

        @Override
        public ShiftSwap save(ShiftSwap s) {
            return toDomain(jpa.save(new ShiftSwapJpaEntity(s.getId(), s.getRequesterId(), s.getRequesterShiftId(),
                    s.getPartnerId(), s.getPartnerShiftId().orElse(null), s.getStatus(), s.getCreatedAt(),
                    s.getResolvedAt().orElse(null))));
        }

        @Override
        public Optional<ShiftSwap> findById(UUID id) {
            return jpa.findById(id).map(ShiftSwaps::toDomain);
        }

        @Override
        public List<ShiftSwap> findInvolving(UUID employeeId) {
            return jpa.findInvolving(employeeId).stream().map(ShiftSwaps::toDomain).toList();
        }

        @Override
        public List<ShiftSwap> findPendingForShifts(Collection<UUID> shiftIds) {
            if (shiftIds.isEmpty()) {
                return List.of();
            }
            return jpa.findByStatusForShifts(SwapStatus.PENDING, shiftIds).stream().map(ShiftSwaps::toDomain).toList();
        }

        private static ShiftSwap toDomain(ShiftSwapJpaEntity e) {
            return ShiftSwap.reconstitute(e.getId(), e.getRequesterId(), e.getRequesterShiftId(), e.getPartnerId(),
                    e.getPartnerShiftId(), e.getStatus(), e.getCreatedAt(), e.getResolvedAt());
        }
    }
}
