package com.viller.gespatiens.diary.infrastructure.web;

import com.viller.gespatiens.diary.application.MonthlyEvaluationsUseCase;
import com.viller.gespatiens.diary.application.MonthlyEvaluationsView;
import com.viller.gespatiens.diary.application.RecordMarkUseCase;
import com.viller.gespatiens.diary.domain.ActivityType;
import com.viller.gespatiens.diary.infrastructure.web.DiaryDayResponse.ResidentResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

// RF6 en tabla mensual (memoria final): las notas de un tipo de actividad durante un mes. Todo el personal la
// consulta; las notas las ponen los educadores y la administración (SecurityConfig)
@RestController
@RequestMapping("/api/diary")
public class MonthlyEvaluationsController {

    private final MonthlyEvaluationsUseCase monthlyEvaluationsUseCase;
    private final RecordMarkUseCase recordMarkUseCase;

    public MonthlyEvaluationsController(MonthlyEvaluationsUseCase monthlyEvaluationsUseCase,
                                        RecordMarkUseCase recordMarkUseCase) {
        this.monthlyEvaluationsUseCase = monthlyEvaluationsUseCase;
        this.recordMarkUseCase = recordMarkUseCase;
    }

    // ?month=2026-10&type=LIFE_SKILLS
    @GetMapping("/evaluations")
    public MonthlyResponse month(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
                                 @RequestParam ActivityType type) {
        return MonthlyResponse.from(monthlyEvaluationsUseCase.execute(month, type));
    }

    // La nota de un residente en una actividad. version: la de la actividad en pantalla (RF8)
    @PutMapping("/activities/{activityId}/evaluations/{patientId}")
    public MarkResponse recordMark(Principal principal, @PathVariable UUID activityId, @PathVariable UUID patientId,
                                   @RequestBody MarkRequest request) {
        return new MarkResponse(recordMarkUseCase.execute(activityId, patientId, request.mark(),
                UUID.fromString(principal.getName()), DiaryRequests.requiredVersion(request.version())));
    }

    // mark null: quitar la nota
    public record MarkRequest(BigDecimal mark, Long version) {
    }

    // La versión nueva de la actividad, para el siguiente cambio
    public record MarkResponse(long version) {
    }

    public record ActivityRefResponse(UUID id, LocalTime startTime, String title, long version) {
    }

    public record DayResponse(LocalDate date, List<ActivityRefResponse> activities) {
    }

    // activityId null: la nota no se cambia desde la tabla (ese día no asistió, o hubo varias actividades)
    public record CellResponse(BigDecimal mark, boolean attended, UUID activityId) {
    }

    public record RowResponse(ResidentResponse resident, List<CellResponse> cells) {
    }

    public record MonthlyResponse(YearMonth month, ActivityType type, List<DayResponse> days,
                                  List<RowResponse> rows) {
        static MonthlyResponse from(MonthlyEvaluationsView view) {
            return new MonthlyResponse(view.month(), view.type(),
                    view.days().stream().map(d -> new DayResponse(d.date(), d.activities().stream()
                            .map(a -> new ActivityRefResponse(a.id(), a.startTime(), a.title().orElse(null),
                                    a.version()))
                            .toList())).toList(),
                    view.rows().stream().map(r -> new RowResponse(
                            new ResidentResponse(r.resident().id(), r.resident().number(), r.resident().name(),
                                    r.resident().surname()),
                            r.cells().stream().map(c -> new CellResponse(c.mark().orElse(null), c.attended(),
                                    c.activityId().orElse(null))).toList())).toList());
        }
    }
}
