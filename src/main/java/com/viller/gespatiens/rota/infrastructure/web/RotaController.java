package com.viller.gespatiens.rota.infrastructure.web;

import com.viller.gespatiens.rota.application.GetRotaUseCase;
import com.viller.gespatiens.rota.application.PlanRotaUseCase;
import com.viller.gespatiens.rota.application.RotaView;
import com.viller.gespatiens.rota.application.SwapView;
import com.viller.gespatiens.rota.application.SwapsUseCase;
import com.viller.gespatiens.rota.domain.Employee;
import com.viller.gespatiens.rota.domain.PlannedShift;
import com.viller.gespatiens.rota.domain.ShiftPart;
import com.viller.gespatiens.rota.domain.SwapStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// El cuadrante de turnos: planificación, no fichajes. Todo el personal lo consulta; lo planifica la
// administración (SecurityConfig) y los cambios de turno los acuerdan los trabajadores entre ellos
@RestController
@RequestMapping("/api/rota")
public class RotaController {

    private final GetRotaUseCase getRotaUseCase;
    private final PlanRotaUseCase planRotaUseCase;
    private final SwapsUseCase swapsUseCase;

    public RotaController(GetRotaUseCase getRotaUseCase, PlanRotaUseCase planRotaUseCase,
                          SwapsUseCase swapsUseCase) {
        this.getRotaUseCase = getRotaUseCase;
        this.planRotaUseCase = planRotaUseCase;
        this.swapsUseCase = swapsUseCase;
    }

    // ?month=2026-10
    @GetMapping
    public RotaResponse month(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return RotaResponse.from(getRotaUseCase.execute(month));
    }

    // ---- Planificar (la administración)

    @PutMapping("/shifts")
    public ShiftResponse assign(@RequestBody AssignRequest request) {
        return ShiftResponse.from(planRotaUseCase.assign(request.date(), request.part(), request.employeeId()));
    }

    @DeleteMapping("/shifts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassign(@PathVariable UUID id) {
        planRotaUseCase.unassign(id);
    }

    @PostMapping("/fill")
    public ChangedResponse fill(@RequestBody FillRequest request) {
        return new ChangedResponse(planRotaUseCase.fill(request.employeeId(), request.from(), request.to(),
                request.weekdays(), request.parts()));
    }

    @PostMapping("/clear")
    public ChangedResponse clear(@RequestBody ClearRequest request) {
        return new ChangedResponse(planRotaUseCase.clear(request.employeeId(), request.from(), request.to()));
    }

    @PostMapping("/repeat-week")
    public ChangedResponse repeatWeek(@RequestBody RepeatWeekRequest request) {
        return new ChangedResponse(planRotaUseCase.repeatWeek(request.sourceMonday(), request.until()));
    }

    @PostMapping("/copy-month")
    public ChangedResponse copyMonth(@RequestBody CopyMonthRequest request) {
        return new ChangedResponse(planRotaUseCase.copyMonth(request.from(), request.to()));
    }

    // ---- Cambios de turno (los trabajadores)

    @GetMapping("/swaps")
    public List<SwapResponse> mySwaps(Principal principal) {
        return swapsUseCase.mine(me(principal)).stream().map(SwapResponse::from).toList();
    }

    @PostMapping("/swaps")
    @ResponseStatus(HttpStatus.CREATED)
    public SwapResponse propose(Principal principal, @RequestBody SwapRequest request) {
        return SwapResponse.from(swapsUseCase.propose(me(principal), request.myShiftId(), request.partnerId(),
                request.partnerShiftId()));
    }

    @PostMapping("/swaps/{id}/accept")
    public SwapResponse accept(Principal principal, @PathVariable UUID id) {
        return SwapResponse.from(swapsUseCase.accept(id, me(principal)));
    }

    @PostMapping("/swaps/{id}/decline")
    public SwapResponse decline(Principal principal, @PathVariable UUID id) {
        return SwapResponse.from(swapsUseCase.decline(id, me(principal)));
    }

    @PostMapping("/swaps/{id}/cancel")
    public SwapResponse cancel(Principal principal, @PathVariable UUID id) {
        return SwapResponse.from(swapsUseCase.cancel(id, me(principal)));
    }

    // CurrentUserJwtConverter pone el id del empleado como "name" de la autenticación
    private static UUID me(Principal principal) {
        return UUID.fromString(principal.getName());
    }

    // ---- DTO

    public record AssignRequest(LocalDate date, ShiftPart part, UUID employeeId) {
    }

    // weekdays: MONDAY..SUNDAY; parts: MORNING, AFTERNOON, NIGHT
    public record FillRequest(UUID employeeId, LocalDate from, LocalDate to, Set<DayOfWeek> weekdays,
                              Set<ShiftPart> parts) {
    }

    public record ClearRequest(UUID employeeId, LocalDate from, LocalDate to) {
    }

    public record RepeatWeekRequest(LocalDate sourceMonday, LocalDate until) {
    }

    // from y to como "2026-09"
    public record CopyMonthRequest(YearMonth from, YearMonth to) {
    }

    // partnerShiftId null: el compañero solo cubre el turno
    public record SwapRequest(UUID myShiftId, UUID partnerId, UUID partnerShiftId) {
    }

    // Cuántos turnos se han añadido o quitado
    public record ChangedResponse(int changed) {
    }

    public record EmployeeResponse(UUID id, String name, String speciality) {
        static EmployeeResponse from(Employee e) {
            return new EmployeeResponse(e.id(), e.name(), e.speciality());
        }
    }

    public record ShiftResponse(UUID id, LocalDate date, ShiftPart part, UUID employeeId) {
        static ShiftResponse from(PlannedShift s) {
            return new ShiftResponse(s.getId(), s.getDate(), s.getPart(), s.getEmployeeId());
        }
    }

    public record RotaResponse(YearMonth month, List<EmployeeResponse> employees, List<ShiftResponse> shifts) {
        static RotaResponse from(RotaView v) {
            return new RotaResponse(v.month(), v.employees().stream().map(EmployeeResponse::from).toList(),
                    v.shifts().stream().map(ShiftResponse::from).toList());
        }
    }

    // Las personas y los turnos pueden faltar (null) si ya no existen
    public record SwapResponse(UUID id, SwapStatus status, EmployeeResponse requester, EmployeeResponse partner,
                               ShiftResponse requesterShift, ShiftResponse partnerShift, Instant createdAt,
                               Instant resolvedAt) {
        static SwapResponse from(SwapView v) {
            return new SwapResponse(v.swap().getId(), v.swap().getStatus(),
                    v.requester().map(EmployeeResponse::from).orElse(null),
                    v.partner().map(EmployeeResponse::from).orElse(null),
                    v.requesterShift().map(ShiftResponse::from).orElse(null),
                    v.partnerShift().map(ShiftResponse::from).orElse(null),
                    v.swap().getCreatedAt(), v.swap().getResolvedAt().orElse(null));
        }
    }
}
