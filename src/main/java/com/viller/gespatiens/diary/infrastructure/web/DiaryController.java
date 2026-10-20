package com.viller.gespatiens.diary.infrastructure.web;

import com.viller.gespatiens.diary.application.DeleteReductionUseCase;
import com.viller.gespatiens.diary.application.GetDiaryDayUseCase;
import com.viller.gespatiens.diary.application.RecordReductionUseCase;
import com.viller.gespatiens.diary.application.RecordShiftUseCase;
import com.viller.gespatiens.diary.domain.DayPart;
import com.viller.gespatiens.diary.infrastructure.web.DiaryDayResponse.ReductionResponse;
import com.viller.gespatiens.diary.infrastructure.web.DiaryDayResponse.ShiftResponse;
import com.viller.gespatiens.diary.infrastructure.web.DiaryRequests.ReductionRequest;
import com.viller.gespatiens.diary.infrastructure.web.DiaryRequests.ShiftRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDate;
import java.util.UUID;

// Una página del diario por fecha (yyyy-MM-dd). Los turnos los rellena cualquier empleado;
// las rebajas, solo el equipo médico (ver SecurityConfig)
@RestController
@RequestMapping("/api/diary/{date}")
public class DiaryController {

    private final GetDiaryDayUseCase getDiaryDayUseCase;
    private final RecordShiftUseCase recordShiftUseCase;
    private final RecordReductionUseCase recordReductionUseCase;
    private final DeleteReductionUseCase deleteReductionUseCase;

    public DiaryController(GetDiaryDayUseCase getDiaryDayUseCase,
                           RecordShiftUseCase recordShiftUseCase,
                           RecordReductionUseCase recordReductionUseCase,
                           DeleteReductionUseCase deleteReductionUseCase) {
        this.getDiaryDayUseCase = getDiaryDayUseCase;
        this.recordShiftUseCase = recordShiftUseCase;
        this.recordReductionUseCase = recordReductionUseCase;
        this.deleteReductionUseCase = deleteReductionUseCase;
    }

    @GetMapping
    public DiaryDayResponse get(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return DiaryDayResponse.from(getDiaryDayUseCase.execute(date));
    }

    @PutMapping("/shifts/{part}")
    public ShiftResponse recordShift(Principal principal,
                                     @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                     @PathVariable DayPart part,
                                     @RequestBody ShiftRequest request) {
        return ShiftResponse.from(recordShiftUseCase.execute(date, part, request.staffIds(), request.notes(),
                currentUserId(principal)));
    }

    @PutMapping("/reductions/{patientId}")
    public ReductionResponse recordReduction(Principal principal,
                                             @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                             @PathVariable UUID patientId,
                                             @RequestBody ReductionRequest request) {
        return ReductionResponse.from(recordReductionUseCase.execute(date, patientId, request.partial(),
                request.total(), request.sport(), currentUserId(principal)));
    }

    @DeleteMapping("/reductions/{patientId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReduction(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                @PathVariable UUID patientId) {
        deleteReductionUseCase.execute(date, patientId);
    }

    // CurrentUserJwtConverter pone el id del empleado como "name" de la autenticación
    private static UUID currentUserId(Principal principal) {
        return UUID.fromString(principal.getName());
    }
}
