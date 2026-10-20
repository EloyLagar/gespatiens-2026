package com.viller.gespatiens.interventions.application;

import com.viller.gespatiens.interventions.domain.Intervention;
import com.viller.gespatiens.interventions.domain.StaffMember;

import java.util.Optional;

// Una intervención tal como la ve un empleado: con su autor resuelto (vacío si ya no está en la plantilla),
// si es suya y si puede modificarla o eliminarla
public record InterventionView(Intervention intervention, Optional<StaffMember> author, boolean mine,
                               boolean manageable) {
}
