package com.viller.gespatiens.rota.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

// Un cambio de turno entre dos trabajadores, sin que intervenga la administración (decisión del centro). Quien lo
// propone ofrece un turno suyo; el compañero lo acepta a cambio de uno de los suyos o, si no se pide nada a
// cambio, simplemente se lo cubre. Al aceptarlo, el cuadrante se actualiza solo
public class ShiftSwap {

    private final UUID id;
    private final UUID requesterId;
    private final UUID requesterShiftId;
    private final UUID partnerId;
    // null: el compañero solo cubre el turno, no da ninguno a cambio
    private final UUID partnerShiftId;
    private SwapStatus status;
    private final Instant createdAt;
    private Instant resolvedAt;

    private ShiftSwap(UUID id, UUID requesterId, UUID requesterShiftId, UUID partnerId, UUID partnerShiftId,
                      SwapStatus status, Instant createdAt, Instant resolvedAt) {
        if (id == null || requesterId == null || requesterShiftId == null || partnerId == null || status == null
                || createdAt == null) {
            throw new IllegalArgumentException("El cambio de turno está incompleto");
        }
        if (requesterId.equals(partnerId)) {
            throw new IllegalArgumentException("Un cambio de turno es con otro compañero");
        }
        this.id = id;
        this.requesterId = requesterId;
        this.requesterShiftId = requesterShiftId;
        this.partnerId = partnerId;
        this.partnerShiftId = partnerShiftId;
        this.status = status;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
    }

    // Proponer el cambio. Solo de turnos propios, de hoy en adelante, y a cambio de uno del compañero si se pide
    public static ShiftSwap propose(PlannedShift mine, UUID requesterId, UUID partnerId, PlannedShift theirs,
                                    Instant now, LocalDate today) {
        if (!mine.isOf(requesterId)) {
            throw new SwapNotAllowedException("Solo puedes proponer cambios de tus propios turnos");
        }
        checkNotPast(mine, today);
        if (theirs != null) {
            if (!theirs.isOf(partnerId)) {
                throw new SwapNotAllowedException("El turno que pides a cambio no es de ese compañero");
            }
            checkNotPast(theirs, today);
        }
        return new ShiftSwap(UUID.randomUUID(), requesterId, mine.getId(), partnerId,
                theirs == null ? null : theirs.getId(), SwapStatus.PENDING, now, null);
    }

    public static ShiftSwap reconstitute(UUID id, UUID requesterId, UUID requesterShiftId, UUID partnerId,
                                         UUID partnerShiftId, SwapStatus status, Instant createdAt,
                                         Instant resolvedAt) {
        return new ShiftSwap(id, requesterId, requesterShiftId, partnerId, partnerShiftId, status, createdAt,
                resolvedAt);
    }

    // Aceptar: solo el compañero, y solo si los turnos siguen siendo de quienes eran cuando se propuso (el
    // cuadrante ha podido cambiar entretanto). Se intercambian y el cambio queda aceptado
    public void accept(UUID by, PlannedShift mine, PlannedShift theirs, Instant now, LocalDate today) {
        ensurePending();
        if (!partnerId.equals(by)) {
            throw new SwapNotAllowedException("Solo el compañero al que se le propone puede aceptarlo");
        }
        if (mine == null || !mine.getId().equals(requesterShiftId) || !mine.isOf(requesterId)
                || (partnerShiftId != null && (theirs == null || !theirs.getId().equals(partnerShiftId)
                || !theirs.isOf(partnerId)))) {
            throw new SwapNotAllowedException("El cuadrante ha cambiado desde que se propuso: ya no se puede aceptar");
        }
        checkNotPast(mine, today);
        if (theirs != null) {
            checkNotPast(theirs, today);
        }
        mine.handOverTo(partnerId);
        if (theirs != null) {
            theirs.handOverTo(requesterId);
        }
        resolve(SwapStatus.ACCEPTED, now);
    }

    public void decline(UUID by, Instant now) {
        ensurePending();
        if (!partnerId.equals(by)) {
            throw new SwapNotAllowedException("Solo el compañero al que se le propone puede rechazarlo");
        }
        resolve(SwapStatus.DECLINED, now);
    }

    // Quien lo propuso lo retira. También lo retira la aplicación si otro cambio aceptado deja sin efecto este
    public void cancel(UUID by, Instant now) {
        ensurePending();
        if (!requesterId.equals(by)) {
            throw new SwapNotAllowedException("Solo quien lo propuso puede retirarlo");
        }
        resolve(SwapStatus.CANCELLED, now);
    }

    public void withdrawBecauseOutdated(Instant now) {
        ensurePending();
        resolve(SwapStatus.CANCELLED, now);
    }

    public boolean involves(UUID shiftId) {
        return requesterShiftId.equals(shiftId) || shiftId.equals(partnerShiftId);
    }

    private void ensurePending() {
        if (status != SwapStatus.PENDING) {
            throw new SwapNotAllowedException("Este cambio de turno ya está resuelto");
        }
    }

    private void resolve(SwapStatus newStatus, Instant now) {
        this.status = newStatus;
        this.resolvedAt = now;
    }

    private static void checkNotPast(PlannedShift shift, LocalDate today) {
        if (shift.getDate().isBefore(today)) {
            throw new SwapNotAllowedException("No se pueden cambiar turnos de días que ya han pasado");
        }
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

    public Optional<UUID> getPartnerShiftId() {
        return Optional.ofNullable(partnerShiftId);
    }

    public SwapStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Optional<Instant> getResolvedAt() {
        return Optional.ofNullable(resolvedAt);
    }
}
