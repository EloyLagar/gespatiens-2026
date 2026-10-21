package com.viller.gespatiens.diary.infrastructure.web;

import com.viller.gespatiens.diary.application.DeleteActivityUseCase;
import com.viller.gespatiens.diary.application.DeleteOutingUseCase;
import com.viller.gespatiens.diary.application.DeleteReductionUseCase;
import com.viller.gespatiens.diary.application.GetDiaryDayUseCase;
import com.viller.gespatiens.diary.application.RecordAttendanceUseCase;
import com.viller.gespatiens.diary.application.RecordEvaluationsUseCase;
import com.viller.gespatiens.diary.application.RecordOutingUseCase;
import com.viller.gespatiens.diary.application.RecordReductionUseCase;
import com.viller.gespatiens.diary.application.RecordShiftUseCase;
import com.viller.gespatiens.diary.application.RescheduleActivityUseCase;
import com.viller.gespatiens.diary.application.ScheduleActivityUseCase;
import com.viller.gespatiens.diary.application.UpdateOutingUseCase;
import com.viller.gespatiens.diary.domain.DayPart;
import com.viller.gespatiens.diary.domain.Evaluation;
import com.viller.gespatiens.diary.infrastructure.web.DiaryDayResponse.ActivityResponse;
import com.viller.gespatiens.diary.infrastructure.web.DiaryDayResponse.OutingResponse;
import com.viller.gespatiens.diary.infrastructure.web.DiaryDayResponse.ReductionResponse;
import com.viller.gespatiens.diary.infrastructure.web.DiaryDayResponse.ShiftResponse;
import com.viller.gespatiens.diary.infrastructure.web.DiaryRequests.ActivityRequest;
import com.viller.gespatiens.diary.infrastructure.web.DiaryRequests.AttendanceRequest;
import com.viller.gespatiens.diary.infrastructure.web.DiaryRequests.EvaluationsRequest;
import com.viller.gespatiens.diary.infrastructure.web.DiaryRequests.OutingRequest;
import com.viller.gespatiens.diary.infrastructure.web.DiaryRequests.OutingUpdateRequest;
import com.viller.gespatiens.diary.infrastructure.web.DiaryRequests.ReductionRequest;
import com.viller.gespatiens.diary.infrastructure.web.DiaryRequests.ShiftRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Una página del diario por fecha (yyyy-MM-dd). Las actividades y salidas se crean en su día y después
// se tratan por id. Todo lo rellena cualquier empleado salvo las rebajas, que son del equipo médico y la
// administración, y las valoraciones, que son de los educadores y la administración (ver SecurityConfig)
@RestController
@RequestMapping("/api/diary")
public class DiaryController {

    private final GetDiaryDayUseCase getDiaryDayUseCase;
    private final RecordShiftUseCase recordShiftUseCase;
    private final RecordReductionUseCase recordReductionUseCase;
    private final DeleteReductionUseCase deleteReductionUseCase;
    private final ScheduleActivityUseCase scheduleActivityUseCase;
    private final RescheduleActivityUseCase rescheduleActivityUseCase;
    private final DeleteActivityUseCase deleteActivityUseCase;
    private final RecordAttendanceUseCase recordAttendanceUseCase;
    private final RecordEvaluationsUseCase recordEvaluationsUseCase;
    private final RecordOutingUseCase recordOutingUseCase;
    private final UpdateOutingUseCase updateOutingUseCase;
    private final DeleteOutingUseCase deleteOutingUseCase;

    public DiaryController(GetDiaryDayUseCase getDiaryDayUseCase,
                           RecordShiftUseCase recordShiftUseCase,
                           RecordReductionUseCase recordReductionUseCase,
                           DeleteReductionUseCase deleteReductionUseCase,
                           ScheduleActivityUseCase scheduleActivityUseCase,
                           RescheduleActivityUseCase rescheduleActivityUseCase,
                           DeleteActivityUseCase deleteActivityUseCase,
                           RecordAttendanceUseCase recordAttendanceUseCase,
                           RecordEvaluationsUseCase recordEvaluationsUseCase,
                           RecordOutingUseCase recordOutingUseCase,
                           UpdateOutingUseCase updateOutingUseCase,
                           DeleteOutingUseCase deleteOutingUseCase) {
        this.getDiaryDayUseCase = getDiaryDayUseCase;
        this.recordShiftUseCase = recordShiftUseCase;
        this.recordReductionUseCase = recordReductionUseCase;
        this.deleteReductionUseCase = deleteReductionUseCase;
        this.scheduleActivityUseCase = scheduleActivityUseCase;
        this.rescheduleActivityUseCase = rescheduleActivityUseCase;
        this.deleteActivityUseCase = deleteActivityUseCase;
        this.recordAttendanceUseCase = recordAttendanceUseCase;
        this.recordEvaluationsUseCase = recordEvaluationsUseCase;
        this.recordOutingUseCase = recordOutingUseCase;
        this.updateOutingUseCase = updateOutingUseCase;
        this.deleteOutingUseCase = deleteOutingUseCase;
    }

    @GetMapping("/{date}")
    public DiaryDayResponse get(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return DiaryDayResponse.from(getDiaryDayUseCase.execute(date));
    }

    @PutMapping("/{date}/shifts/{part}")
    public ShiftResponse recordShift(Principal principal,
                                     @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                     @PathVariable DayPart part,
                                     @RequestBody ShiftRequest request) {
        return ShiftResponse.from(recordShiftUseCase.execute(date, part, request.staffIds(), request.notes(),
                currentUserId(principal), DiaryRequests.requiredVersion(request.version())));
    }

    @PutMapping("/{date}/reductions/{patientId}")
    public ReductionResponse recordReduction(Principal principal,
                                             @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                             @PathVariable UUID patientId,
                                             @RequestBody ReductionRequest request) {
        return ReductionResponse.from(recordReductionUseCase.execute(date, patientId, request.partial(),
                request.total(), request.sport(), currentUserId(principal)));
    }

    @DeleteMapping("/{date}/reductions/{patientId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReduction(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                @PathVariable UUID patientId) {
        deleteReductionUseCase.execute(date, patientId);
    }

    @PostMapping("/{date}/activities")
    @ResponseStatus(HttpStatus.CREATED)
    public ActivityResponse scheduleActivity(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                             @RequestBody ActivityRequest request) {
        return ActivityResponse.from(scheduleActivityUseCase.execute(date, request.startTime(), request.type(),
                request.title()));
    }

    @PutMapping("/activities/{activityId}")
    public ActivityResponse rescheduleActivity(Principal principal, @PathVariable UUID activityId,
                                               @RequestBody ActivityRequest request) {
        return ActivityResponse.from(rescheduleActivityUseCase.execute(activityId, request.startTime(),
                request.type(), request.title(), currentUserId(principal), DiaryRequests.requiredVersion(request.version())));
    }

    @DeleteMapping("/activities/{activityId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteActivity(Principal principal, @PathVariable UUID activityId) {
        deleteActivityUseCase.execute(activityId, currentUserId(principal));
    }

    @PutMapping("/activities/{activityId}/attendance")
    public ActivityResponse recordAttendance(Principal principal, @PathVariable UUID activityId,
                                             @RequestBody AttendanceRequest request) {
        return ActivityResponse.from(recordAttendanceUseCase.execute(activityId, request.attendance(),
                currentUserId(principal), DiaryRequests.requiredVersion(request.version())));
    }

    @PutMapping("/activities/{activityId}/evaluations")
    public ActivityResponse recordEvaluations(Principal principal, @PathVariable UUID activityId,
                                              @RequestBody EvaluationsRequest request) {
        Map<UUID, Evaluation> evaluations = new HashMap<>();
        if (request.evaluations() != null) {
            request.evaluations().forEach((patientId, e) -> {
                if (e != null && (e.mark() != null || e.satisfaction() != null || e.utility() != null)) {
                    evaluations.put(patientId, new Evaluation(e.mark(), e.satisfaction(), e.utility()));
                }
            });
        }
        return ActivityResponse.from(recordEvaluationsUseCase.execute(activityId, evaluations,
                currentUserId(principal), DiaryRequests.requiredVersion(request.version())));
    }

    @PostMapping("/{date}/outings")
    @ResponseStatus(HttpStatus.CREATED)
    public OutingResponse recordOuting(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                       @RequestBody OutingRequest request) {
        return OutingResponse.from(recordOutingUseCase.execute(date, request.patientId(), request.leftAt(),
                request.notes()));
    }

    @PutMapping("/outings/{outingId}")
    public OutingResponse updateOuting(@PathVariable UUID outingId, @RequestBody OutingUpdateRequest request) {
        return OutingResponse.from(updateOutingUseCase.execute(outingId, request.leftAt(), request.returnedAt(),
                request.notes()));
    }

    @DeleteMapping("/outings/{outingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOuting(@PathVariable UUID outingId) {
        deleteOutingUseCase.execute(outingId);
    }

    // CurrentUserJwtConverter pone el id del empleado como "name" de la autenticación
    private static UUID currentUserId(Principal principal) {
        return UUID.fromString(principal.getName());
    }
}
