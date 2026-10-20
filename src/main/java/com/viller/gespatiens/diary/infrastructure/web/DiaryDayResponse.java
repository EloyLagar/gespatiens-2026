package com.viller.gespatiens.diary.infrastructure.web;

import com.viller.gespatiens.diary.application.DiaryDayView;
import com.viller.gespatiens.diary.application.DiaryDayView.ReductionView;
import com.viller.gespatiens.diary.application.DiaryDayView.ShiftView;
import com.viller.gespatiens.diary.domain.DayPart;
import com.viller.gespatiens.diary.domain.Resident;
import com.viller.gespatiens.diary.domain.StaffMember;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// DTO de salida de una página del diario
public record DiaryDayResponse(LocalDate date, List<ShiftResponse> shifts, List<ReductionResponse> reductions) {

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

    public static DiaryDayResponse from(DiaryDayView view) {
        return new DiaryDayResponse(
                view.date(),
                view.shifts().stream().map(ShiftResponse::from).toList(),
                view.reductions().stream().map(ReductionResponse::from).toList());
    }
}
