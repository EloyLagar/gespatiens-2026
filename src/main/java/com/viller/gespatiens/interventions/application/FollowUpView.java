package com.viller.gespatiens.interventions.application;

import com.viller.gespatiens.interventions.domain.Resident;
import com.viller.gespatiens.interventions.domain.StaffMember;

import java.util.List;

// El seguimiento de un residente tal como lo ve un empleado: sus tutores, sus intervenciones (de la más
// reciente a la más antigua) y si quien mira es tutor suyo, que es lo que le permite registrar intervenciones
public record FollowUpView(Resident resident, List<StaffMember> tutors, List<InterventionView> interventions,
                           boolean viewerIsTutor) {
}
