package com.viller.gespatiens.interventions.application;

import com.viller.gespatiens.interventions.domain.Intervention;
import com.viller.gespatiens.interventions.domain.ResidentTutors;
import com.viller.gespatiens.interventions.domain.StaffDirectory;
import com.viller.gespatiens.interventions.domain.StaffMember;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

// Arma las vistas del módulo. Lo comparten el listado, el seguimiento de un residente y los casos de uso
// que devuelven una intervención recién guardada
final class FollowUpViews {

    private FollowUpViews() {
    }

    static boolean isAdministrator(StaffDirectory staff, UUID employeeId) {
        return staff.find(employeeId).map(StaffMember::administrator).orElse(false);
    }

    static Map<UUID, StaffMember> staffOf(StaffDirectory staff, Collection<UUID> ids) {
        return staff.findAll(ids.stream().distinct().toList());
    }

    // Los tutores por nombre. Si alguno ya no está en la plantilla, no aparece
    static List<StaffMember> tutors(ResidentTutors tutors, Map<UUID, StaffMember> staff) {
        return tutors.getTutorIds().stream()
                .map(staff::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(StaffMember::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    static InterventionView of(Intervention intervention, Map<UUID, StaffMember> staff, UUID viewerId,
                               boolean viewerIsAdmin) {
        return new InterventionView(
                intervention,
                intervention.getAuthorId().map(staff::get),
                intervention.getAuthorId().map(viewerId::equals).orElse(false),
                intervention.canBeManagedBy(viewerId, viewerIsAdmin));
    }
}
