package com.viller.gespatiens.rota.infrastructure;

import com.viller.gespatiens.rota.domain.ShiftPart;
import com.viller.gespatiens.rota.domain.SwapStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// Las tablas del cuadrante
public final class RotaJpaEntities {

    private RotaJpaEntities() {
    }

    @Entity(name = "PlannedShiftJpaEntity")
    @Table(name = "planned_shifts")
    public static class PlannedShiftJpaEntity {

        @Id
        private UUID id;

        @Column(nullable = false)
        private LocalDate date;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 10)
        private ShiftPart part;

        @Column(nullable = false)
        private UUID employeeId;

        protected PlannedShiftJpaEntity() {
        }

        public PlannedShiftJpaEntity(UUID id, LocalDate date, ShiftPart part, UUID employeeId) {
            this.id = id;
            this.date = date;
            this.part = part;
            this.employeeId = employeeId;
        }

        public UUID getId() {
            return id;
        }

        public LocalDate getDate() {
            return date;
        }

        public ShiftPart getPart() {
            return part;
        }

        public UUID getEmployeeId() {
            return employeeId;
        }
    }

    @Entity(name = "ShiftSwapJpaEntity")
    @Table(name = "shift_swaps")
    public static class ShiftSwapJpaEntity {

        @Id
        private UUID id;

        @Column(nullable = false)
        private UUID requesterId;

        @Column(nullable = false)
        private UUID requesterShiftId;

        @Column(nullable = false)
        private UUID partnerId;

        private UUID partnerShiftId;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 10)
        private SwapStatus status;

        @Column(nullable = false)
        private Instant createdAt;

        private Instant resolvedAt;

        protected ShiftSwapJpaEntity() {
        }

        public ShiftSwapJpaEntity(UUID id, UUID requesterId, UUID requesterShiftId, UUID partnerId,
                                  UUID partnerShiftId, SwapStatus status, Instant createdAt, Instant resolvedAt) {
            this.id = id;
            this.requesterId = requesterId;
            this.requesterShiftId = requesterShiftId;
            this.partnerId = partnerId;
            this.partnerShiftId = partnerShiftId;
            this.status = status;
            this.createdAt = createdAt;
            this.resolvedAt = resolvedAt;
        }

        public UUID getId() {
            return id;
        }

        public UUID getRequesterId() {
            return requesterId;
        }

        public UUID getRequesterShiftId() {
            return requesterShiftId;
        }

        public UUID getPartnerId() {
            return partnerId;
        }

        public UUID getPartnerShiftId() {
            return partnerShiftId;
        }

        public SwapStatus getStatus() {
            return status;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public Instant getResolvedAt() {
            return resolvedAt;
        }
    }
}
