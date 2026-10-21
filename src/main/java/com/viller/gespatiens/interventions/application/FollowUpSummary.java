package com.viller.gespatiens.interventions.application;

import com.viller.gespatiens.interventions.domain.Resident;
import com.viller.gespatiens.interventions.domain.StaffMember;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// Un residente en el listado de intervenciones: sus tutores, cuántas intervenciones tiene y cuándo fue la última
public record FollowUpSummary(Resident resident, List<StaffMember> tutors, long interventionCount,
                              Optional<LocalDate> lastIntervention, boolean viewerIsTutor) {
}
