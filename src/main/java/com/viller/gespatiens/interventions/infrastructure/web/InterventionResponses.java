package com.viller.gespatiens.interventions.infrastructure.web;

import com.viller.gespatiens.interventions.application.FollowUpSummary;
import com.viller.gespatiens.interventions.application.FollowUpView;
import com.viller.gespatiens.interventions.application.InterventionView;
import com.viller.gespatiens.interventions.domain.Intervention;
import com.viller.gespatiens.interventions.domain.Resident;
import com.viller.gespatiens.interventions.domain.StaffMember;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// DTO de salida de las intervenciones
public final class InterventionResponses {

    private InterventionResponses() {
    }

    public record StaffResponse(UUID id, String name, String speciality) {
        static StaffResponse from(StaffMember member) {
            return new StaffResponse(member.id(), member.name(), member.speciality());
        }

        static List<StaffResponse> from(List<StaffMember> members) {
            return members.stream().map(StaffResponse::from).toList();
        }
    }

    // number null: ya no está en el centro
    public record ResidentResponse(UUID id, Integer number, String name, String surname, boolean active) {
        static ResidentResponse from(Resident resident) {
            return new ResidentResponse(resident.id(), resident.number(), resident.name(), resident.surname(),
                    resident.active());
        }
    }

    // lastIntervention null: todavía no tiene ninguna
    public record FollowUpSummaryResponse(ResidentResponse resident, List<StaffResponse> tutors, long interventionCount,
                                          LocalDate lastIntervention, boolean mine) {
        public static FollowUpSummaryResponse from(FollowUpSummary summary) {
            return new FollowUpSummaryResponse(ResidentResponse.from(summary.resident()),
                    StaffResponse.from(summary.tutors()), summary.interventionCount(),
                    summary.lastIntervention().orElse(null), summary.viewerIsTutor());
        }
    }

    // author null: el empleado ya no está en la plantilla. mine: la registró quien mira.
    // manageable: puede modificarla y eliminarla (su autor o la administración)
    public record InterventionResponse(UUID id, LocalDate date, String text, Instant createdAt, Instant editedAt,
                                       StaffResponse author, boolean mine, boolean manageable) {
        public static InterventionResponse from(InterventionView view) {
            Intervention intervention = view.intervention();
            return new InterventionResponse(intervention.getId(), intervention.getDate(), intervention.getText(),
                    intervention.getCreatedAt(), intervention.getEditedAt().orElse(null),
                    view.author().map(StaffResponse::from).orElse(null), view.mine(), view.manageable());
        }
    }

    // canRecord: quien mira es tutor del residente y este sigue en el centro
    public record FollowUpResponse(ResidentResponse resident, List<StaffResponse> tutors,
                                   List<InterventionResponse> interventions, boolean tutor, boolean canRecord) {
        public static FollowUpResponse from(FollowUpView view) {
            return new FollowUpResponse(ResidentResponse.from(view.resident()), StaffResponse.from(view.tutors()),
                    view.interventions().stream().map(InterventionResponse::from).toList(),
                    view.viewerIsTutor(), view.viewerIsTutor() && view.resident().active());
        }
    }
}
