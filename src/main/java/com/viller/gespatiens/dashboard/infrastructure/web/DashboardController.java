package com.viller.gespatiens.dashboard.infrastructure.web;

import com.viller.gespatiens.dashboard.application.DashboardQueries.ShiftCount;
import com.viller.gespatiens.dashboard.application.DashboardUseCase;
import com.viller.gespatiens.dashboard.application.DashboardView;
import com.viller.gespatiens.dashboard.application.Deadline;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

// El panel de control de la administración (memoria final, 5.1). Solo la administración (SecurityConfig)
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardUseCase dashboardUseCase;

    public DashboardController(DashboardUseCase dashboardUseCase) {
        this.dashboardUseCase = dashboardUseCase;
    }

    // ?month=2026-10
    @GetMapping
    public DashboardResponse get(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return DashboardResponse.from(dashboardUseCase.execute(month));
    }

    public record ShiftResponse(UUID employeeId, String name, String speciality, int morning, int afternoon,
                                int night, int total) {
        static ShiftResponse from(ShiftCount c) {
            return new ShiftResponse(c.employeeId(), c.name(), c.speciality(), c.morning(), c.afternoon(), c.night(),
                    c.total());
        }
    }

    public record WeekResponse(LocalDate monday, int notices) {
    }

    public record DeadlineResponse(Deadline.Kind kind, Deadline.Resident resident, LocalDate dueDate, long daysLeft,
                                   boolean overdue, UUID reportId, String detail) {
        static DeadlineResponse from(Deadline d) {
            return new DeadlineResponse(d.kind(), d.resident(), d.dueDate(), d.daysLeft(), d.overdue(), d.reportId(),
                    d.detail());
        }
    }

    public record DashboardResponse(YearMonth month, int occupiedPlaces, int totalPlaces,
                                    int residentsWithoutBothTutors, int reportsInProgress, int interventions,
                                    List<ShiftResponse> shifts, List<WeekResponse> noticesByWeek,
                                    List<DeadlineResponse> deadlines) {
        static DashboardResponse from(DashboardView v) {
            return new DashboardResponse(v.month(), v.occupiedPlaces(), v.totalPlaces(),
                    v.residentsWithoutBothTutors(), v.reportsInProgress(), v.interventions(),
                    v.shifts().stream().map(ShiftResponse::from).toList(),
                    v.noticesByWeek().stream().map(w -> new WeekResponse(w.monday(), w.notices())).toList(),
                    v.deadlines().stream().map(DeadlineResponse::from).toList());
        }
    }
}
