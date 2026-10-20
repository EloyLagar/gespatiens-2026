package com.viller.gespatiens.diary.infrastructure.web;

import com.viller.gespatiens.diary.application.DiaryDayView;
import com.viller.gespatiens.diary.application.DiaryDayView.ActivityView;
import com.viller.gespatiens.diary.application.DiaryDayView.AttendanceRow;
import com.viller.gespatiens.diary.application.DiaryDayView.EvaluationAverages;
import com.viller.gespatiens.diary.application.DiaryDayView.OutingView;
import com.viller.gespatiens.diary.application.DiaryDayView.ReductionView;
import com.viller.gespatiens.diary.application.DiaryDayView.ShiftView;
import com.viller.gespatiens.diary.domain.ActivityType;
import com.viller.gespatiens.diary.domain.AttendanceStatus;
import com.viller.gespatiens.diary.domain.DayPart;
import com.viller.gespatiens.diary.domain.Evaluation;
import com.viller.gespatiens.diary.domain.Resident;
import com.viller.gespatiens.diary.domain.StaffMember;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

// DTO de salida de una página del diario
public record DiaryDayResponse(LocalDate date, List<ShiftResponse> shifts, List<ReductionResponse> reductions,
                               List<ActivityResponse> activities, List<OutingResponse> outings) {

    public record StaffResponse(UUID id, String name, String speciality) {
        static StaffResponse from(StaffMember member) {
            return new StaffResponse(member.id(), member.name(), member.speciality());
        }
    }

    // updatedAt null: nadie ha rellenado todavía ese turno
    public record ShiftResponse(DayPart part, List<StaffResponse> staff, String notes,
                                StaffResponse updatedBy, Instant updatedAt) {
        static ShiftResponse from(ShiftView view) {
            return new ShiftResponse(
                    view.part(),
                    view.staff().stream().map(StaffResponse::from).toList(),
                    view.notes().orElse(null),
                    view.updatedBy().map(StaffResponse::from).orElse(null),
                    view.updatedAt().orElse(null));
        }
    }

    public record ResidentResponse(UUID id, Integer number, String name, String surname) {
        static ResidentResponse from(Resident resident) {
            return new ResidentResponse(resident.id(), resident.number(), resident.name(), resident.surname());
        }
    }

    public record ReductionResponse(ResidentResponse resident, String partial, boolean total, boolean sport) {
        static ReductionResponse from(ReductionView view) {
            return new ReductionResponse(ResidentResponse.from(view.resident()), view.partial().orElse(null),
                    view.total(), view.sport());
        }
    }

    // Una valoración o las medias de una actividad: lo que no se tenga va a null
    public record EvaluationResponse(BigDecimal mark, BigDecimal satisfaction, BigDecimal utility) {
        static EvaluationResponse from(Evaluation evaluation) {
            return new EvaluationResponse(evaluation.mark(), evaluation.satisfaction(), evaluation.utility());
        }

        static EvaluationResponse from(EvaluationAverages averages) {
            return new EvaluationResponse(averages.mark().orElse(null), averages.satisfaction().orElse(null),
                    averages.utility().orElse(null));
        }
    }

    // status null: sin anotar (o rebajado, si reduced). evaluation null: sin valorar
    public record AttendanceResponse(ResidentResponse resident, AttendanceStatus status, boolean reduced,
                                     String partialReduction, EvaluationResponse evaluation) {
        static AttendanceResponse from(AttendanceRow row) {
            return new AttendanceResponse(ResidentResponse.from(row.resident()), row.status().orElse(null),
                    row.reduced(), row.partialReduction().orElse(null),
                    row.evaluation().map(EvaluationResponse::from).orElse(null));
        }
    }

    public record ActivityResponse(UUID id, LocalTime startTime, ActivityType type, String title,
                                   List<AttendanceResponse> attendance, EvaluationResponse averages) {
        static ActivityResponse from(ActivityView view) {
            return new ActivityResponse(view.id(), view.startTime(), view.type(), view.title().orElse(null),
                    view.attendance().stream().map(AttendanceResponse::from).toList(),
                    EvaluationResponse.from(view.averages()));
        }
    }

    // returnedAt null: todavía no ha vuelto
    public record OutingResponse(UUID id, ResidentResponse resident, LocalTime leftAt, LocalTime returnedAt,
                                 String notes) {
        static OutingResponse from(OutingView view) {
            return new OutingResponse(view.id(), ResidentResponse.from(view.resident()), view.leftAt(),
                    view.returnedAt().orElse(null), view.notes().orElse(null));
        }
    }

    public static DiaryDayResponse from(DiaryDayView view) {
        return new DiaryDayResponse(
                view.date(),
                view.shifts().stream().map(ShiftResponse::from).toList(),
                view.reductions().stream().map(ReductionResponse::from).toList(),
                view.activities().stream().map(ActivityResponse::from).toList(),
                view.outings().stream().map(OutingResponse::from).toList());
    }
}
